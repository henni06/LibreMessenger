package org.briarproject.bramble.plugin.bluetooth;

import org.briarproject.bramble.api.FormatException;
import org.briarproject.bramble.api.Pair;
import org.briarproject.bramble.api.data.BdfList;
import org.briarproject.bramble.api.event.Event;
import org.briarproject.bramble.api.event.EventListener;
import org.briarproject.bramble.api.io.TimeoutMonitor;
import org.briarproject.bramble.api.keyagreement.KeyAgreementConnection;
import org.briarproject.bramble.api.keyagreement.KeyAgreementListener;
import org.briarproject.bramble.api.keyagreement.event.KeyAgreementListeningEvent;
import org.briarproject.bramble.api.keyagreement.event.KeyAgreementStoppedListeningEvent;
import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.bramble.api.plugin.Backoff;
import org.briarproject.bramble.api.plugin.ConnectionHandler;
import org.briarproject.bramble.api.plugin.PluginCallback;
import org.briarproject.bramble.api.plugin.PluginException;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.plugin.duplex.DuplexPlugin;
import org.briarproject.bramble.api.plugin.duplex.DuplexTransportConnection;
import org.briarproject.bramble.api.plugin.event.BluetoothEnabledEvent;
import org.briarproject.bramble.api.plugin.event.DisableBluetoothEvent;
import org.briarproject.bramble.api.plugin.event.EnableBluetoothEvent;
import org.briarproject.bramble.api.properties.TransportProperties;
import org.briarproject.bramble.api.rendezvous.KeyMaterialSource;
import org.briarproject.bramble.api.rendezvous.RendezvousEndpoint;
import org.briarproject.bramble.api.settings.Settings;
import org.briarproject.bramble.api.settings.event.SettingsUpdatedEvent;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.briarproject.bramble.api.keyagreement.KeyAgreementConstants.TRANSPORT_ID_BLUETOOTH;
import static org.briarproject.bramble.api.plugin.BluetoothConstants.ID;
import static org.briarproject.bramble.api.plugin.BluetoothConstants.PREF_BT_ENABLE;
import static org.briarproject.bramble.api.plugin.BluetoothConstants.PROP_ADDRESS;
import static org.briarproject.bramble.api.plugin.BluetoothConstants.PROP_UUID;
import static org.briarproject.bramble.api.plugin.BluetoothConstants.UUID_BYTES;
import static org.briarproject.bramble.util.LogUtils.logException;
import static org.briarproject.bramble.util.PrivacyUtils.scrubMacAddress;
import static org.briarproject.bramble.util.StringUtils.isNullOrEmpty;
import static org.briarproject.bramble.util.StringUtils.macToBytes;
import static org.briarproject.bramble.util.StringUtils.macToString;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
abstract class BluetoothPlugin<SS> implements DuplexPlugin, EventListener {

	private static final Logger LOG =
			getLogger(BluetoothPlugin.class.getName());

	final BluetoothConnectionLimiter connectionLimiter;
	final TimeoutMonitor timeoutMonitor;

	private final Executor ioExecutor;
	private final SecureRandom secureRandom;
	private final Backoff backoff;
	private final PluginCallback callback;
	private final int maxLatency, maxIdleTime;
	private final AtomicBoolean used = new AtomicBoolean(false);

	private volatile boolean running = false, contactConnections = false;
	private volatile String contactConnectionsUuid = null;
	private volatile SS socket = null;

	abstract void initialiseAdapter() throws IOException;

	abstract boolean isAdapterEnabled();

	abstract void enableAdapter();

	abstract void disableAdapterIfEnabledByUs();

	abstract void setEnabledByUs();

	/**
	 * Returns the local Bluetooth address, or null if no valid address can
	 * be found.
	 */
	@Nullable
	abstract String getBluetoothAddress();

	abstract SS openServerSocket(String uuid) throws IOException;

	abstract void tryToClose(@Nullable SS ss);

	abstract DuplexTransportConnection acceptConnection(SS ss)
			throws IOException;

	abstract boolean isValidAddress(String address);

	abstract DuplexTransportConnection connectTo(String address, String uuid)
			throws IOException;

	@Nullable
	abstract DuplexTransportConnection discoverAndConnect(String uuid);

	BluetoothPlugin(BluetoothConnectionLimiter connectionLimiter,
			TimeoutMonitor timeoutMonitor, Executor ioExecutor,
			SecureRandom secureRandom, Backoff backoff,
			PluginCallback callback, int maxLatency, int maxIdleTime) {
		this.connectionLimiter = connectionLimiter;
		this.timeoutMonitor = timeoutMonitor;
		this.ioExecutor = ioExecutor;
		this.secureRandom = secureRandom;
		this.backoff = backoff;
		this.callback = callback;
		this.maxLatency = maxLatency;
		this.maxIdleTime = maxIdleTime;
	}

	void onAdapterEnabled() {
		LOG.info("Bluetooth enabled");
		// We may not have been able to get the local address before
		ioExecutor.execute(this::updateProperties);
		if (shouldAllowContactConnections()) bind();
	}

	void onAdapterDisabled() {
		LOG.info("Bluetooth disabled");
		tryToClose(socket);
		connectionLimiter.allConnectionsClosed();
		callback.transportDisabled();
	}

	@Override
	public TransportId getId() {
		return ID;
	}

	@Override
	public int getMaxLatency() {
		return maxLatency;
	}

	@Override
	public int getMaxIdleTime() {
		return maxIdleTime;
	}

	@Override
	public void start() throws PluginException {
		if (used.getAndSet(true)) throw new IllegalStateException();
		try {
			initialiseAdapter();
		} catch (IOException e) {
			throw new PluginException(e);
		}
		updateProperties();
		running = true;
		loadSettings(callback.getSettings());
		if (shouldAllowContactConnections()) {
			if (isAdapterEnabled()) bind();
			else enableAdapter();
		}
	}

	private void loadSettings(Settings settings) {
		contactConnections = settings.getBoolean(PREF_BT_ENABLE, false);
	}

	private boolean shouldAllowContactConnections() {
		return contactConnections;
	}

	private void bind() {
		ioExecutor.execute(() -> {
			if (!isRunning() || !shouldAllowContactConnections()) return;
			// Bind a server socket to accept connections from contacts
			SS ss;
			try {
				ss = openServerSocket(contactConnectionsUuid);
			} catch (IOException e) {
				logException(LOG, WARNING, e);
				return;
			}
			if (!isRunning() || !shouldAllowContactConnections()) {
				tryToClose(ss);
				return;
			}
			socket = ss;
			backoff.reset();
			callback.transportEnabled();
			acceptContactConnections();
		});
	}

	private void updateProperties() {
		TransportProperties p = callback.getLocalProperties();
		String address = p.get(PROP_ADDRESS);
		String uuid = p.get(PROP_UUID);
		boolean changed = false;
		if (address == null) {
			address = getBluetoothAddress();
			if (LOG.isLoggable(INFO))
				LOG.info("Local address " + scrubMacAddress(address));
			if (!isNullOrEmpty(address)) {
				p.put(PROP_ADDRESS, address);
				changed = true;
			}
		}
		if (uuid == null) {
			byte[] random = new byte[UUID_BYTES];
			secureRandom.nextBytes(random);
			uuid = UUID.nameUUIDFromBytes(random).toString();
			p.put(PROP_UUID, uuid);
			changed = true;
		}
		contactConnectionsUuid = uuid;
		if (changed) callback.mergeLocalProperties(p);
	}

	private void acceptContactConnections() {
		while (true) {
			DuplexTransportConnection conn;
			try {
				conn = acceptConnection(socket);
			} catch (IOException e) {
				// This is expected when the socket is closed
				if (LOG.isLoggable(INFO)) LOG.info(e.toString());
				return;
			}
			LOG.info("Connection received");
			connectionLimiter.connectionOpened(conn);
			backoff.reset();
			callback.handleConnection(conn);
			if (!running) return;
		}
	}

	@Override
	public void stop() {
		running = false;
		tryToClose(socket);
		callback.transportDisabled();
		disableAdapterIfEnabledByUs();
	}

	@Override
	public boolean isRunning() {
		return running && isAdapterEnabled();
	}

	@Override
	public boolean shouldPoll() {
		return true;
	}

	@Override
	public int getPollingInterval() {
		return backoff.getPollingInterval();
	}

	@Override
	public void poll(Collection<Pair<TransportProperties, ConnectionHandler>>
			properties) {
		if (!isRunning() || !shouldAllowContactConnections()) return;
		backoff.increment();
		for (Pair<TransportProperties, ConnectionHandler> p : properties) {
			connect(p.getFirst(), p.getSecond());
		}
	}

	private void connect(TransportProperties p, ConnectionHandler h) {
		String address = p.get(PROP_ADDRESS);
		if (isNullOrEmpty(address)) return;
		String uuid = p.get(PROP_UUID);
		if (isNullOrEmpty(uuid)) return;
		ioExecutor.execute(() -> {
			DuplexTransportConnection d = createConnection(p);
			if (d != null) {
				backoff.reset();
				h.handleConnection(d);
			}
		});
	}

	@Nullable
	private DuplexTransportConnection connect(String address, String uuid) {
		// Validate the address
		if (!isValidAddress(address)) {
			if (LOG.isLoggable(WARNING))
				// Not scrubbing here to be able to figure out the problem
				LOG.warning("Invalid address " + address);
			return null;
		}
		// Validate the UUID
		try {
			//noinspection ResultOfMethodCallIgnored
			UUID.fromString(uuid);
		} catch (IllegalArgumentException e) {
			if (LOG.isLoggable(WARNING)) LOG.warning("Invalid UUID " + uuid);
			return null;
		}
		if (LOG.isLoggable(INFO))
			LOG.info("Connecting to " + scrubMacAddress(address));
		try {
			DuplexTransportConnection conn = connectTo(address, uuid);
			if (LOG.isLoggable(INFO))
				LOG.info("Connected to " + scrubMacAddress(address));
			return conn;
		} catch (IOException e) {
			if (LOG.isLoggable(INFO))
				LOG.info("Could not connect to " + scrubMacAddress(address));
			return null;
		}
	}

	@Override
	public DuplexTransportConnection createConnection(TransportProperties p) {
		if (!isRunning() || !shouldAllowContactConnections()) return null;
		if (!connectionLimiter.canOpenContactConnection()) return null;
		String address = p.get(PROP_ADDRESS);
		if (isNullOrEmpty(address)) return null;
		String uuid = p.get(PROP_UUID);
		if (isNullOrEmpty(uuid)) return null;
		DuplexTransportConnection conn = connect(address, uuid);
		if (conn != null) connectionLimiter.connectionOpened(conn);
		return conn;
	}

	@Override
	public boolean supportsKeyAgreement() {
		return true;
	}

	@Override
	public KeyAgreementListener createKeyAgreementListener(byte[] commitment) {
		if (!isRunning()) return null;
		// No truncation necessary because COMMIT_LENGTH = 16
		String uuid = UUID.nameUUIDFromBytes(commitment).toString();
		if (LOG.isLoggable(INFO)) LOG.info("Key agreement UUID " + uuid);
		// Bind a server socket for receiving key agreement connections
		SS ss;
		try {
			ss = openServerSocket(uuid);
		} catch (IOException e) {
			logException(LOG, WARNING, e);
			return null;
		}
		if (!isRunning()) {
			tryToClose(ss);
			return null;
		}
		BdfList descriptor = new BdfList();
		descriptor.add(TRANSPORT_ID_BLUETOOTH);
		String address = getBluetoothAddress();
		if (address != null) descriptor.add(macToBytes(address));
		return new BluetoothKeyAgreementListener(descriptor, ss);
	}

	@Override
	public DuplexTransportConnection createKeyAgreementConnection(
			byte[] commitment, BdfList descriptor) {
		if (!isRunning()) return null;
		// No truncation necessary because COMMIT_LENGTH = 16
		String uuid = UUID.nameUUIDFromBytes(commitment).toString();
		DuplexTransportConnection conn;
		if (descriptor.size() == 1) {
			if (LOG.isLoggable(INFO))
				LOG.info("Discovering address for key agreement UUID " + uuid);
			conn = discoverAndConnect(uuid);
		} else {
			String address;
			try {
				address = parseAddress(descriptor);
			} catch (FormatException e) {
				LOG.info("Invalid address in key agreement descriptor");
				return null;
			}
			if (LOG.isLoggable(INFO))
				LOG.info("Connecting to key agreement UUID " + uuid);
			conn = connect(address, uuid);
		}
		if (conn != null) connectionLimiter.connectionOpened(conn);
		return conn;
	}

	private String parseAddress(BdfList descriptor) throws FormatException {
		byte[] mac = descriptor.getRaw(1);
		if (mac.length != 6) throw new FormatException();
		return macToString(mac);
	}

	@Override
	public boolean supportsRendezvous() {
		return false;
	}

	@Override
	public RendezvousEndpoint createRendezvousEndpoint(KeyMaterialSource k,
			boolean alice, ConnectionHandler incoming) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof EnableBluetoothEvent) {
			ioExecutor.execute(this::enableAdapter);
		} else if (e instanceof DisableBluetoothEvent) {
			ioExecutor.execute(this::disableAdapterIfEnabledByUs);
		} else if (e instanceof BluetoothEnabledEvent) {
			setEnabledByUs();
		} else if (e instanceof SettingsUpdatedEvent) {
			SettingsUpdatedEvent s = (SettingsUpdatedEvent) e;
			if (s.getNamespace().equals(ID.getString()))
				ioExecutor.execute(() -> onSettingsUpdated(s.getSettings()));
		} else if (e instanceof KeyAgreementListeningEvent) {
			ioExecutor.execute(connectionLimiter::keyAgreementStarted);
		} else if (e instanceof KeyAgreementStoppedListeningEvent) {
			ioExecutor.execute(connectionLimiter::keyAgreementEnded);
		}
	}

	private void onSettingsUpdated(Settings settings) {
		boolean wasAllowed = shouldAllowContactConnections();
		loadSettings(settings);
		boolean isAllowed = shouldAllowContactConnections();
		if (wasAllowed && !isAllowed) {
			LOG.info("Contact connections disabled");
			tryToClose(socket);
			callback.transportDisabled();
			disableAdapterIfEnabledByUs();
		} else if (!wasAllowed && isAllowed) {
			LOG.info("Contact connections enabled");
			if (isAdapterEnabled()) bind();
			else enableAdapter();
		}
	}

	private class BluetoothKeyAgreementListener extends KeyAgreementListener {

		private final SS ss;

		private BluetoothKeyAgreementListener(BdfList descriptor, SS ss) {
			super(descriptor);
			this.ss = ss;
		}

		@Override
		public KeyAgreementConnection accept() throws IOException {
			DuplexTransportConnection conn = acceptConnection(ss);
			if (LOG.isLoggable(INFO)) LOG.info(ID + ": Incoming connection");
			connectionLimiter.connectionOpened(conn);
			return new KeyAgreementConnection(conn, ID);
		}

		@Override
		public void close() {
			tryToClose(ss);
		}
	}
}

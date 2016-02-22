package org.briarproject.plugins.bluetooth;

import org.briarproject.api.TransportId;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.crypto.PseudoRandom;
import org.briarproject.api.plugins.Backoff;
import org.briarproject.api.plugins.duplex.DuplexPlugin;
import org.briarproject.api.plugins.duplex.DuplexPluginCallback;
import org.briarproject.api.plugins.duplex.DuplexTransportConnection;
import org.briarproject.api.properties.TransportProperties;
import org.briarproject.api.system.Clock;
import org.briarproject.util.LatchedReference;
import org.briarproject.util.OsUtils;
import org.briarproject.util.StringUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static javax.bluetooth.DiscoveryAgent.GIAC;

class BluetoothPlugin implements DuplexPlugin {

	// Share an ID with the Android Bluetooth plugin
	static final TransportId ID = new TransportId("bt");

	private static final Logger LOG =
			Logger.getLogger(BluetoothPlugin.class.getName());
	private static final int UUID_BYTES = 16;

	private final Executor ioExecutor;
	private final SecureRandom secureRandom;
	private final Clock clock;
	private final Backoff backoff;
	private final DuplexPluginCallback callback;
	private final int maxLatency;
	private final Semaphore discoverySemaphore = new Semaphore(1);

	private volatile boolean running = false;
	private volatile StreamConnectionNotifier socket = null;
	private volatile LocalDevice localDevice = null;

	BluetoothPlugin(Executor ioExecutor, SecureRandom secureRandom, Clock clock,
			Backoff backoff, DuplexPluginCallback callback, int maxLatency) {
		this.ioExecutor = ioExecutor;
		this.secureRandom = secureRandom;
		this.clock = clock;
		this.backoff = backoff;
		this.callback = callback;
		this.maxLatency = maxLatency;
	}

	public TransportId getId() {
		return ID;
	}

	public int getMaxLatency() {
		return maxLatency;
	}

	public int getMaxIdleTime() {
		// Bluetooth detects dead connections so we don't need keepalives
		return Integer.MAX_VALUE;
	}

	public boolean start() throws IOException {
		// Initialise the Bluetooth stack
		try {
			localDevice = LocalDevice.getLocalDevice();
		} catch (UnsatisfiedLinkError e) {
			// On Linux the user may need to install libbluetooth-dev
			if (OsUtils.isLinux())
				callback.showMessage("BLUETOOTH_INSTALL_LIBS");
			return false;
		}
		if (LOG.isLoggable(INFO))
			LOG.info("Local address " + localDevice.getBluetoothAddress());
		running = true;
		bind();
		return true;
	}

	private void bind() {
		ioExecutor.execute(new Runnable() {
			public void run() {
				if (!running) return;
				// Advertise the Bluetooth address to contacts
				TransportProperties p = new TransportProperties();
				p.put("address", localDevice.getBluetoothAddress());
				callback.mergeLocalProperties(p);
				// Bind a server socket to accept connections from contacts
				String url = makeUrl("localhost", getUuid());
				StreamConnectionNotifier ss;
				try {
					ss = (StreamConnectionNotifier) Connector.open(url);
				} catch (IOException e) {
					if (LOG.isLoggable(WARNING))
						LOG.log(WARNING, e.toString(), e);
					return;
				}
				if (!running) {
					tryToClose(ss);
					return;
				}
				socket = ss;
				backoff.reset();
				callback.transportEnabled();
				acceptContactConnections(ss);
			}
		});
	}

	private String makeUrl(String address, String uuid) {
		return "btspp://" + address + ":" + uuid + ";name=RFCOMM";
	}

	private String getUuid() {
		String uuid = callback.getLocalProperties().get("uuid");
		if (uuid == null) {
			byte[] random = new byte[UUID_BYTES];
			secureRandom.nextBytes(random);
			uuid = UUID.nameUUIDFromBytes(random).toString();
			TransportProperties p = new TransportProperties();
			p.put("uuid", uuid);
			callback.mergeLocalProperties(p);
		}
		return uuid;
	}

	private void tryToClose(StreamConnectionNotifier ss) {
		try {
			if (ss != null) ss.close();
		} catch (IOException e) {
			if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
		} finally {
			callback.transportDisabled();
		}
	}

	private void acceptContactConnections(StreamConnectionNotifier ss) {
		while (true) {
			StreamConnection s;
			try {
				s = ss.acceptAndOpen();
			} catch (IOException e) {
				// This is expected when the socket is closed
				if (LOG.isLoggable(INFO)) LOG.info(e.toString());
				return;
			}
			backoff.reset();
			callback.incomingConnectionCreated(wrapSocket(s));
			if (!running) return;
		}
	}

	private DuplexTransportConnection wrapSocket(StreamConnection s) {
		return new BluetoothTransportConnection(this, s);
	}

	public void stop() {
		running = false;
		tryToClose(socket);
	}

	public boolean isRunning() {
		return running;
	}

	public boolean shouldPoll() {
		return true;
	}

	public int getPollingInterval() {
		return backoff.getPollingInterval();
	}

	public void poll(final Collection<ContactId> connected) {
		if (!running) return;
		backoff.increment();
		// Try to connect to known devices in parallel
		Map<ContactId, TransportProperties> remote =
				callback.getRemoteProperties();
		for (Entry<ContactId, TransportProperties> e : remote.entrySet()) {
			final ContactId c = e.getKey();
			if (connected.contains(c)) continue;
			final String address = e.getValue().get("address");
			if (StringUtils.isNullOrEmpty(address)) continue;
			final String uuid = e.getValue().get("uuid");
			if (StringUtils.isNullOrEmpty(uuid)) continue;
			ioExecutor.execute(new Runnable() {
				public void run() {
					if (!running) return;
					StreamConnection s = connect(makeUrl(address, uuid));
					if (s != null) {
						backoff.reset();
						callback.outgoingConnectionCreated(c, wrapSocket(s));
					}
				}
			});
		}
	}

	private StreamConnection connect(String url) {
		if (LOG.isLoggable(INFO)) LOG.info("Connecting to " + url);
		try {
			StreamConnection s = (StreamConnection) Connector.open(url);
			if (LOG.isLoggable(INFO)) LOG.info("Connected to " + url);
			return s;
		} catch (IOException e) {
			if (LOG.isLoggable(INFO)) LOG.info("Could not connect to " + url);
			return null;
		}
	}

	public DuplexTransportConnection createConnection(ContactId c) {
		if (!running) return null;
		TransportProperties p = callback.getRemoteProperties().get(c);
		if (p == null) return null;
		String address = p.get("address");
		if (StringUtils.isNullOrEmpty(address)) return null;
		String uuid = p.get("uuid");
		if (StringUtils.isNullOrEmpty(uuid)) return null;
		String url = makeUrl(address, uuid);
		StreamConnection s = connect(url);
		if (s == null) return null;
		return new BluetoothTransportConnection(this, s);
	}

	public boolean supportsInvitations() {
		return true;
	}

	public DuplexTransportConnection createInvitationConnection(PseudoRandom r,
			long timeout) {
		if (!running) return null;
		// Use the invitation codes to generate the UUID
		byte[] b = r.nextBytes(UUID_BYTES);
		String uuid = UUID.nameUUIDFromBytes(b).toString();
		String url = makeUrl("localhost", uuid);
		// Make the device discoverable if possible
		makeDeviceDiscoverable();
		// Bind a server socket for receiving invitation connections
		final StreamConnectionNotifier ss;
		try {
			ss = (StreamConnectionNotifier) Connector.open(url);
		} catch (IOException e) {
			if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
			return null;
		}
		if (!running) {
			tryToClose(ss);
			return null;
		}
		// Start the background threads
		LatchedReference<StreamConnection> socketLatch =
				new LatchedReference<StreamConnection>();
		new DiscoveryThread(socketLatch, uuid, timeout).start();
		new BluetoothListenerThread(socketLatch, ss).start();
		// Wait for an incoming or outgoing connection
		try {
			StreamConnection s = socketLatch.waitForReference(timeout);
			if (s != null) return new BluetoothTransportConnection(this, s);
		} catch (InterruptedException e) {
			LOG.warning("Interrupted while exchanging invitations");
			Thread.currentThread().interrupt();
		} finally {
			// Closing the socket will terminate the listener thread
			tryToClose(ss);
		}
		return null;
	}

	private void makeDeviceDiscoverable() {
		// Try to make the device discoverable (requires root on Linux)
		try {
			localDevice.setDiscoverable(GIAC);
		} catch (BluetoothStateException e) {
			if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
		}
	}

	private class DiscoveryThread extends Thread {

		private final LatchedReference<StreamConnection> socketLatch;
		private final String uuid;
		private final long timeout;

		private DiscoveryThread(LatchedReference<StreamConnection> socketLatch,
				String uuid, long timeout) {
			this.socketLatch = socketLatch;
			this.uuid = uuid;
			this.timeout = timeout;
		}

		@Override
		public void run() {
			DiscoveryAgent discoveryAgent = localDevice.getDiscoveryAgent();
			long now = clock.currentTimeMillis();
			long end = now + timeout;
			while (now < end && running && !socketLatch.isSet()) {
				if (!discoverySemaphore.tryAcquire()) {
					LOG.info("Another device discovery is in progress");
					return;
				}
				try {
					InvitationListener listener =
							new InvitationListener(discoveryAgent, uuid);
					discoveryAgent.startInquiry(GIAC, listener);
					String url = listener.waitForUrl();
					if (url == null) continue;
					StreamConnection s = connect(url);
					if (s == null) continue;
					LOG.info("Outgoing connection");
					if (!socketLatch.set(s)) {
						LOG.info("Closing redundant connection");
						tryToClose(s);
					}
					return;
				} catch (BluetoothStateException e) {
					if (LOG.isLoggable(WARNING))
						LOG.log(WARNING, e.toString(), e);
					return;
				} catch (InterruptedException e) {
					LOG.warning("Interrupted while waiting for URL");
					Thread.currentThread().interrupt();
					return;
				} finally {
					discoverySemaphore.release();
				}
			}
		}

		private void tryToClose(StreamConnection s) {
			try {
				if (s != null) s.close();
			} catch (IOException e) {
				if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
			}
		}
	}

	private static class BluetoothListenerThread extends Thread {

		private final LatchedReference<StreamConnection> socketLatch;
		private final StreamConnectionNotifier serverSocket;

		private BluetoothListenerThread(
				LatchedReference<StreamConnection> socketLatch,
				StreamConnectionNotifier serverSocket) {
			this.socketLatch = socketLatch;
			this.serverSocket = serverSocket;
		}

		@Override
		public void run() {
			LOG.info("Listening for invitation connections");
			// Listen until a connection is received or the socket is closed
			try {
				StreamConnection s = serverSocket.acceptAndOpen();
				LOG.info("Incoming connection");
				if (!socketLatch.set(s)) {
					LOG.info("Closing redundant connection");
					s.close();
				}
			} catch (IOException e) {
				// This is expected when the socket is closed
				if (LOG.isLoggable(INFO)) LOG.info(e.toString());
			}
		}
	}
}

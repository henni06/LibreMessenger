package org.libreproject.bramble.plugin.tor;

import android.app.Application;

import org.libreproject.bramble.api.battery.BatteryManager;
import org.libreproject.bramble.api.crypto.CryptoComponent;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.lifecycle.IoExecutor;
import org.libreproject.bramble.api.network.NetworkManager;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.Backoff;
import org.libreproject.bramble.api.plugin.BackoffFactory;
import org.libreproject.bramble.api.plugin.PluginCallback;
import org.libreproject.bramble.api.plugin.TorConstants;
import org.libreproject.bramble.api.plugin.TorControlPort;
import org.libreproject.bramble.api.plugin.TorDirectory;
import org.libreproject.bramble.api.plugin.TorSocksPort;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.plugin.duplex.DuplexPlugin;
import org.libreproject.bramble.api.plugin.duplex.DuplexPluginFactory;
import org.libreproject.bramble.api.system.AndroidWakeLockManager;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.bramble.api.system.LocationUtils;
import org.libreproject.bramble.api.system.ResourceProvider;
import org.libreproject.bramble.api.system.WakefulIoExecutor;
import org.libreproject.bramble.util.AndroidUtils;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;
import javax.net.SocketFactory;

@Immutable
@NotNullByDefault
public class AndroidTorPluginFactory implements DuplexPluginFactory {

	private static final Logger LOG =
			Logger.getLogger(AndroidTorPluginFactory.class.getName());

	private static final int MAX_LATENCY = 30 * 1000; // 30 seconds
	private static final int MAX_IDLE_TIME = 30 * 1000; // 30 seconds
	private static final int MIN_POLLING_INTERVAL = 60 * 1000; // 1 minute
	private static final int MAX_POLLING_INTERVAL = 10 * 60 * 1000; // 10 mins
	private static final double BACKOFF_BASE = 1.2;

	private final Executor ioExecutor, wakefulIoExecutor;
	private final Application app;
	private final NetworkManager networkManager;
	private final LocationUtils locationUtils;
	private final EventBus eventBus;
	private final SocketFactory torSocketFactory;
	private final BackoffFactory backoffFactory;
	private final ResourceProvider resourceProvider;
	private final CircumventionProvider circumventionProvider;
	private final BatteryManager batteryManager;
	private final AndroidWakeLockManager wakeLockManager;
	private final Clock clock;
	private final File torDirectory;
	private int torSocksPort;
	private int torControlPort;
	private final CryptoComponent crypto;

	@Inject
	AndroidTorPluginFactory(@IoExecutor Executor ioExecutor,
			@WakefulIoExecutor Executor wakefulIoExecutor,
			Application app,
			NetworkManager networkManager,
			LocationUtils locationUtils,
			EventBus eventBus,
			SocketFactory torSocketFactory,
			BackoffFactory backoffFactory,
			ResourceProvider resourceProvider,
			CircumventionProvider circumventionProvider,
			BatteryManager batteryManager,
			AndroidWakeLockManager wakeLockManager,
			Clock clock,
			@TorDirectory File torDirectory,
			@TorSocksPort int torSocksPort,
			@TorControlPort int torControlPort,
			CryptoComponent crypto) {
		this.ioExecutor = ioExecutor;
		this.wakefulIoExecutor = wakefulIoExecutor;
		this.app = app;
		this.networkManager = networkManager;
		this.locationUtils = locationUtils;
		this.eventBus = eventBus;
		this.torSocketFactory = torSocketFactory;
		this.backoffFactory = backoffFactory;
		this.resourceProvider = resourceProvider;
		this.circumventionProvider = circumventionProvider;
		this.batteryManager = batteryManager;
		this.wakeLockManager = wakeLockManager;
		this.clock = clock;
		this.torDirectory = torDirectory;
		this.torSocksPort = torSocksPort;
		this.torControlPort = torControlPort;
		this.crypto = crypto;
	}

	@Override
	public TransportId getId() {
		return TorConstants.ID;
	}

	@Override
	public long getMaxLatency() {
		return MAX_LATENCY;
	}

	@Override
	public DuplexPlugin createPlugin(PluginCallback callback) {

		// Check that we have a Tor binary for this architecture
		String architecture = null;
		for (String abi : AndroidUtils.getSupportedArchitectures()) {
			if (abi.startsWith("x86_64")) {
				architecture = "x86_64";
				break;
			} else if (abi.startsWith("x86")) {
				architecture = "x86";
				break;
			} else if (abi.startsWith("arm64")) {
				architecture = "arm64";
				break;
			} else if (abi.startsWith("armeabi")) {
				architecture = "arm";
				break;
			}
		}
		if (architecture == null) {
			LOG.info("Tor is not supported on this architecture");
			return null;
		}
		// Use position-independent executable
		architecture += "_pie";

		Backoff backoff = backoffFactory.createBackoff(MIN_POLLING_INTERVAL,
				MAX_POLLING_INTERVAL, BACKOFF_BASE);
		TorRendezvousCrypto torRendezvousCrypto =
				new TorRendezvousCryptoImpl(crypto);
		AndroidTorPlugin plugin = new AndroidTorPlugin(ioExecutor,
				wakefulIoExecutor, app, networkManager, locationUtils,
				torSocketFactory, clock, resourceProvider,
				circumventionProvider, batteryManager, wakeLockManager,
				backoff, torRendezvousCrypto, callback, architecture,
				MAX_LATENCY, MAX_IDLE_TIME, torDirectory, torSocksPort,
				torControlPort);
		eventBus.addListener(plugin);
		return plugin;
	}
}

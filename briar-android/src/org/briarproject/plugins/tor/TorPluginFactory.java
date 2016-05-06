package org.briarproject.plugins.tor;

import android.content.Context;
import android.os.Build;

import org.briarproject.android.util.AndroidUtils;
import org.briarproject.api.TransportId;
import org.briarproject.api.event.EventBus;
import org.briarproject.api.plugins.Backoff;
import org.briarproject.api.plugins.BackoffFactory;
import org.briarproject.api.plugins.duplex.DuplexPlugin;
import org.briarproject.api.plugins.duplex.DuplexPluginCallback;
import org.briarproject.api.plugins.duplex.DuplexPluginFactory;
import org.briarproject.api.reporting.DevReporter;
import org.briarproject.api.system.LocationUtils;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class TorPluginFactory implements DuplexPluginFactory {

	private static final Logger LOG =
			Logger.getLogger(TorPluginFactory.class.getName());

	private static final int MAX_LATENCY = 30 * 1000; // 30 seconds
	private static final int MAX_IDLE_TIME = 30 * 1000; // 30 seconds
	private static final int MIN_POLLING_INTERVAL = 2 * 60 * 1000; // 2 minutes
	private static final int MAX_POLLING_INTERVAL = 60 * 60 * 1000; // 1 hour
	private static final double BACKOFF_BASE = 1.2;

	private final Executor ioExecutor;
	private final Context appContext;
	private final LocationUtils locationUtils;
	private final DevReporter reporter;
	private final EventBus eventBus;
	private final BackoffFactory backoffFactory;

	public TorPluginFactory(Executor ioExecutor, Context appContext,
			LocationUtils locationUtils, DevReporter reporter,
			EventBus eventBus, BackoffFactory backoffFactory) {
		this.ioExecutor = ioExecutor;
		this.appContext = appContext;
		this.locationUtils = locationUtils;
		this.reporter = reporter;
		this.eventBus = eventBus;
		this.backoffFactory = backoffFactory;
	}

	public TransportId getId() {
		return TorPlugin.ID;
	}

	public int getMaxLatency() {
		return MAX_LATENCY;
	}

	public DuplexPlugin createPlugin(DuplexPluginCallback callback) {

		// Check that we have a Tor binary for this architecture
		String architecture = null;
		for (String abi : AndroidUtils.getSupportedArchitectures()) {
			if (abi.startsWith("x86")) {
				architecture = "x86";
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
		// Use position-independent executable for SDK >= 16
		if (Build.VERSION.SDK_INT >= 16) architecture += "-pie";

		Backoff backoff = backoffFactory.createBackoff(MIN_POLLING_INTERVAL,
				MAX_POLLING_INTERVAL, BACKOFF_BASE);
		TorPlugin plugin = new TorPlugin(ioExecutor, appContext, locationUtils,
				reporter, backoff, callback, architecture, MAX_LATENCY,
				MAX_IDLE_TIME);
		eventBus.addListener(plugin);
		return plugin;
	}
}

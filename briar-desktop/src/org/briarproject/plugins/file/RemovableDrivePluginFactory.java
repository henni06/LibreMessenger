package org.briarproject.plugins.file;

import org.briarproject.api.TransportId;
import org.briarproject.api.plugins.simplex.SimplexPlugin;
import org.briarproject.api.plugins.simplex.SimplexPluginCallback;
import org.briarproject.api.plugins.simplex.SimplexPluginFactory;
import org.briarproject.util.OsUtils;

import java.util.concurrent.Executor;

public class RemovableDrivePluginFactory implements SimplexPluginFactory {

	// Maximum latency 14 days (Royal Mail or lackadaisical carrier pigeon)
	private static final int MAX_LATENCY = 14 * 24 * 60 * 60 * 1000;
	private static final int POLLING_INTERVAL = 10 * 1000; // 10 seconds

	private final Executor ioExecutor;

	public RemovableDrivePluginFactory(Executor ioExecutor) {
		this.ioExecutor = ioExecutor;
	}

	public TransportId getId() {
		return RemovableDrivePlugin.ID;
	}

	public int getMaxLatency() {
		return MAX_LATENCY;
	}

	public SimplexPlugin createPlugin(SimplexPluginCallback callback) {
		RemovableDriveFinder finder;
		RemovableDriveMonitor monitor;
		if (OsUtils.isLinux()) {
			finder = new LinuxRemovableDriveFinder();
			monitor = new LinuxRemovableDriveMonitor();
		} else if (OsUtils.isMacLeopardOrNewer()) {
			finder = new MacRemovableDriveFinder();
			monitor = new MacRemovableDriveMonitor();
		} else if (OsUtils.isMac()) {
			// JNotify requires OS X 10.5 or newer, so we have to poll
			finder = new MacRemovableDriveFinder();
			monitor = new PollingRemovableDriveMonitor(ioExecutor, finder,
					POLLING_INTERVAL);
		} else if (OsUtils.isWindows()) {
			finder = new WindowsRemovableDriveFinder();
			monitor = new PollingRemovableDriveMonitor(ioExecutor, finder,
					POLLING_INTERVAL);
		} else {
			return null;
		}
		return new RemovableDrivePlugin(ioExecutor, callback, finder, monitor,
				MAX_LATENCY);
	}
}

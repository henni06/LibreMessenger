package org.briarproject.bramble.plugin.tor;

import org.briarproject.bramble.api.battery.BatteryManager;
import org.briarproject.bramble.api.network.NetworkManager;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.plugin.Backoff;
import org.briarproject.bramble.api.plugin.PluginCallback;
import org.briarproject.bramble.api.system.Clock;
import org.briarproject.bramble.api.system.LocationUtils;
import org.briarproject.bramble.api.system.ResourceProvider;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.concurrent.Executor;

import javax.net.SocketFactory;

@NotNullByDefault
abstract class JavaTorPlugin extends TorPlugin {

	JavaTorPlugin(Executor ioExecutor, NetworkManager networkManager,
			LocationUtils locationUtils, SocketFactory torSocketFactory,
			Clock clock, ResourceProvider resourceProvider,
			CircumventionProvider circumventionProvider,
			BatteryManager batteryManager, Backoff backoff,
			PluginCallback callback, String architecture, int maxLatency,
			int maxIdleTime, File torDirectory) {
		super(ioExecutor, networkManager, locationUtils, torSocketFactory,
				clock, resourceProvider, circumventionProvider, batteryManager,
				backoff, callback, architecture, maxLatency, maxIdleTime,
				torDirectory);
	}

	@Override
	protected long getLastUpdateTime() {
		CodeSource codeSource =
				getClass().getProtectionDomain().getCodeSource();
		if (codeSource == null) throw new AssertionError("CodeSource null");
		try {
			URI path = codeSource.getLocation().toURI();
			File file = new File(path);
			return file.lastModified();
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
	}
}

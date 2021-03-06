package org.libreproject.bramble.plugin.file;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.PluginCallback;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.plugin.simplex.SimplexPlugin;
import org.libreproject.bramble.api.plugin.simplex.SimplexPluginFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.libreproject.bramble.api.plugin.file.RemovableDriveConstants.ID;

@Immutable
@NotNullByDefault
public class RemovableDrivePluginFactory implements SimplexPluginFactory {

	static final long MAX_LATENCY = DAYS.toMillis(28);

	@Inject
	RemovableDrivePluginFactory() {
	}

	@Override
	public TransportId getId() {
		return ID;
	}

	@Override
	public long getMaxLatency() {
		return MAX_LATENCY;
	}

	@Nullable
	@Override
	public SimplexPlugin createPlugin(PluginCallback callback) {
		return new RemovableDrivePlugin(callback, MAX_LATENCY);
	}
}

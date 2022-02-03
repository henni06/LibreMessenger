package org.libreproject.bramble.plugin.tor;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.Plugin.State;
import org.libreproject.bramble.api.plugin.PluginCallback;
import org.libreproject.bramble.api.plugin.TransportConnectionReader;
import org.libreproject.bramble.api.plugin.TransportConnectionWriter;
import org.libreproject.bramble.api.plugin.duplex.DuplexTransportConnection;
import org.libreproject.bramble.api.properties.TransportProperties;
import org.libreproject.bramble.api.settings.Settings;

import java.util.Collection;

import static java.util.Collections.emptyList;

@NotNullByDefault
public class TestPluginCallback implements PluginCallback {

	@Override
	public Settings getSettings() {
		return new Settings();
	}

	@Override
	public TransportProperties getLocalProperties() {
		return new TransportProperties();
	}

	@Override
	public Collection<TransportProperties> getRemoteProperties() {
		return emptyList();
	}

	@Override
	public void mergeSettings(Settings s) {
	}

	@Override
	public void mergeLocalProperties(TransportProperties p) {
	}

	@Override
	public void pluginStateChanged(State state) {
	}

	@Override
	public void handleConnection(DuplexTransportConnection c) {
	}

	@Override
	public void handleReader(TransportConnectionReader r) {
	}

	@Override
	public void handleWriter(TransportConnectionWriter w) {
	}
}

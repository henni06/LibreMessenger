package org.libreproject.bramble.plugin.bluetooth;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.duplex.DuplexPlugin;
import org.libreproject.bramble.api.plugin.duplex.DuplexTransportConnection;

import javax.annotation.Nullable;

@NotNullByDefault
public interface BluetoothPlugin extends DuplexPlugin {

	boolean isDiscovering();

	void disablePolling();

	void enablePolling();

	@Nullable
	DuplexTransportConnection discoverAndConnectForSetup(String uuid);

	void stopDiscoverAndConnect();
}

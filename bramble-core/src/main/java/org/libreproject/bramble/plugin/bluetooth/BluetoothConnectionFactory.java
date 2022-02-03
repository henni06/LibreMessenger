package org.libreproject.bramble.plugin.bluetooth;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.duplex.DuplexPlugin;
import org.libreproject.bramble.api.plugin.duplex.DuplexTransportConnection;

import java.io.IOException;

@NotNullByDefault
interface BluetoothConnectionFactory<S> {

	DuplexTransportConnection wrapSocket(DuplexPlugin plugin, S socket)
			throws IOException;
}

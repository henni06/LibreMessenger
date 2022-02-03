package org.libreproject.bramble.plugin.modem;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
interface SerialPortList {

	String[] getPortNames();
}

package org.libreproject.bramble.plugin.modem;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
class SerialPortListImpl implements SerialPortList {

	@Override
	public String[] getPortNames() {
		return jssc.SerialPortList.getPortNames();
	}
}

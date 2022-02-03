package org.libreproject.bramble.plugin.modem;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
interface ModemFactory {

	Modem createModem(Modem.Callback callback, String portName);
}

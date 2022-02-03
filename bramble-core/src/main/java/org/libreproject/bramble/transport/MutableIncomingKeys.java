package org.libreproject.bramble.transport;

import org.libreproject.bramble.api.crypto.SecretKey;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.transport.IncomingKeys;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
@NotNullByDefault
class MutableIncomingKeys {

	private final SecretKey tagKey, headerKey;
	private final long timePeriod;
	private final ReorderingWindow window;

	MutableIncomingKeys(IncomingKeys in) {
		tagKey = in.getTagKey();
		headerKey = in.getHeaderKey();
		timePeriod = in.getTimePeriod();
		window = new ReorderingWindow(in.getWindowBase(), in.getWindowBitmap());
	}

	IncomingKeys snapshot() {
		return new IncomingKeys(tagKey, headerKey, timePeriod,
				window.getBase(), window.getBitmap());
	}

	SecretKey getTagKey() {
		return tagKey;
	}

	SecretKey getHeaderKey() {
		return headerKey;
	}

	long getTimePeriod() {
		return timePeriod;
	}

	ReorderingWindow getWindow() {
		return window;
	}
}

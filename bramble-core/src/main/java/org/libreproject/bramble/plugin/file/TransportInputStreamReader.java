package org.libreproject.bramble.plugin.file;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportConnectionReader;

import java.io.InputStream;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.libreproject.bramble.util.IoUtils.tryToClose;

@NotNullByDefault
class TransportInputStreamReader implements TransportConnectionReader {

	private static final Logger LOG =
			getLogger(TransportInputStreamReader.class.getName());

	private final InputStream in;

	TransportInputStreamReader(InputStream in) {
		this.in = in;
	}

	@Override
	public InputStream getInputStream() {
		return in;
	}

	@Override
	public void dispose(boolean exception, boolean recognised) {
		tryToClose(in, LOG, WARNING);
	}
}

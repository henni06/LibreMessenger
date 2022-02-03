package org.libreproject.bramble.api.reliability;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.IOException;

@NotNullByDefault
public interface ReadHandler {

	void handleRead(byte[] b) throws IOException;
}

package org.libreproject.bramble.api.crypto;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.IOException;

@NotNullByDefault
public interface StreamEncrypter {

	/**
	 * Encrypts the given frame and writes it to the stream.
	 */
	void writeFrame(byte[] payload, int payloadLength, int paddingLength,
			boolean finalFrame) throws IOException;

	/**
	 * Flushes the stream.
	 */
	void flush() throws IOException;
}

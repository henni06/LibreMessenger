package org.libreproject.bramble.api.crypto;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.transport.StreamContext;

import java.io.OutputStream;

@NotNullByDefault
public interface StreamEncrypterFactory {

	/**
	 * Creates a {@link StreamEncrypter} for encrypting a transport stream.
	 */
	StreamEncrypter createStreamEncrypter(OutputStream out, StreamContext ctx);

	/**
	 * Creates a {@link StreamEncrypter} for encrypting a contact exchange
	 * stream.
	 */
	StreamEncrypter createContactExchangeStreamEncrypter(OutputStream out,
			SecretKey headerKey);

	/**
	 * Creates a {@link StreamEncrypter} for encrypting a log stream.
	 */
	StreamEncrypter createLogStreamEncrypter(OutputStream out,
			SecretKey headerKey);
}

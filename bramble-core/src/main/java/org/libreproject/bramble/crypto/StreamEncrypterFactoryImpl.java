package org.libreproject.bramble.crypto;

import org.libreproject.bramble.api.crypto.CryptoComponent;
import org.libreproject.bramble.api.crypto.SecretKey;
import org.libreproject.bramble.api.crypto.StreamEncrypter;
import org.libreproject.bramble.api.crypto.StreamEncrypterFactory;
import org.libreproject.bramble.api.crypto.TransportCrypto;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.transport.StreamContext;

import java.io.OutputStream;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;
import javax.inject.Provider;

import static org.libreproject.bramble.api.transport.TransportConstants.PROTOCOL_VERSION;
import static org.libreproject.bramble.api.transport.TransportConstants.STREAM_HEADER_NONCE_LENGTH;
import static org.libreproject.bramble.api.transport.TransportConstants.TAG_LENGTH;

@Immutable
@NotNullByDefault
class StreamEncrypterFactoryImpl implements StreamEncrypterFactory {

	private final CryptoComponent crypto;
	private final TransportCrypto transportCrypto;
	private final Provider<AuthenticatedCipher> cipherProvider;

	@Inject
	StreamEncrypterFactoryImpl(CryptoComponent crypto,
			TransportCrypto transportCrypto,
			Provider<AuthenticatedCipher> cipherProvider) {
		this.crypto = crypto;
		this.transportCrypto = transportCrypto;
		this.cipherProvider = cipherProvider;
	}

	@Override
	public StreamEncrypter createStreamEncrypter(OutputStream out,
			StreamContext ctx) {
		AuthenticatedCipher cipher = cipherProvider.get();
		long streamNumber = ctx.getStreamNumber();
		byte[] tag = new byte[TAG_LENGTH];
		transportCrypto.encodeTag(tag, ctx.getTagKey(), PROTOCOL_VERSION,
				streamNumber);
		byte[] streamHeaderNonce = new byte[STREAM_HEADER_NONCE_LENGTH];
		crypto.getSecureRandom().nextBytes(streamHeaderNonce);
		SecretKey frameKey = crypto.generateSecretKey();
		return new StreamEncrypterImpl(out, cipher, streamNumber, tag,
				streamHeaderNonce, ctx.getHeaderKey(), frameKey);
	}

	@Override
	public StreamEncrypter createContactExchangeStreamEncrypter(
			OutputStream out, SecretKey headerKey) {
		AuthenticatedCipher cipher = cipherProvider.get();
		byte[] streamHeaderNonce = new byte[STREAM_HEADER_NONCE_LENGTH];
		crypto.getSecureRandom().nextBytes(streamHeaderNonce);
		SecretKey frameKey = crypto.generateSecretKey();
		return new StreamEncrypterImpl(out, cipher, 0, null, streamHeaderNonce,
				headerKey, frameKey);
	}

	@Override
	public StreamEncrypter createLogStreamEncrypter(OutputStream out,
			SecretKey headerKey) {
		return createContactExchangeStreamEncrypter(out, headerKey);
	}
}

package org.libreproject.bramble.identity;

import org.libreproject.bramble.api.crypto.CryptoComponent;
import org.libreproject.bramble.api.crypto.KeyPair;
import org.libreproject.bramble.api.crypto.PrivateKey;
import org.libreproject.bramble.api.crypto.PublicKey;
import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.identity.AuthorFactory;
import org.libreproject.bramble.api.identity.AuthorId;
import org.libreproject.bramble.api.identity.LocalAuthor;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.libreproject.bramble.api.identity.Author.FORMAT_VERSION;
import static org.libreproject.bramble.api.identity.AuthorId.LABEL;
import static org.libreproject.bramble.util.ByteUtils.INT_32_BYTES;
import static org.libreproject.bramble.util.ByteUtils.writeUint32;
import static org.libreproject.bramble.util.StringUtils.toUtf8;

@Immutable
@NotNullByDefault
class AuthorFactoryImpl implements AuthorFactory {

	private final CryptoComponent crypto;

	@Inject
	AuthorFactoryImpl(CryptoComponent crypto) {
		this.crypto = crypto;
	}

	@Override
	public Author createAuthor(String name, PublicKey publicKey) {
		return createAuthor(FORMAT_VERSION, name, publicKey);
	}

	@Override
	public Author createAuthor(int formatVersion, String name,
			PublicKey publicKey) {
		AuthorId id = getId(formatVersion, name, publicKey);
		return new Author(id, formatVersion, name, publicKey);
	}

	@Override
	public LocalAuthor createLocalAuthor(String name) {
		KeyPair signatureKeyPair = crypto.generateSignatureKeyPair();
		PublicKey publicKey = signatureKeyPair.getPublic();
		PrivateKey privateKey = signatureKeyPair.getPrivate();
		AuthorId id = getId(FORMAT_VERSION, name, publicKey);
		return new LocalAuthor(id, FORMAT_VERSION, name, publicKey, privateKey);
	}

	private AuthorId getId(int formatVersion, String name,
			PublicKey publicKey) {
		byte[] formatVersionBytes = new byte[INT_32_BYTES];
		writeUint32(formatVersion, formatVersionBytes, 0);
		return new AuthorId(crypto.hash(LABEL, formatVersionBytes,
				toUtf8(name), publicKey.getEncoded()));
	}
}

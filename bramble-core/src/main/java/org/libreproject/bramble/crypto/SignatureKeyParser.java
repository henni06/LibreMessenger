package org.libreproject.bramble.crypto;

import org.libreproject.bramble.api.crypto.KeyParser;
import org.libreproject.bramble.api.crypto.PrivateKey;
import org.libreproject.bramble.api.crypto.PublicKey;
import org.libreproject.bramble.api.crypto.SignaturePrivateKey;
import org.libreproject.bramble.api.crypto.SignaturePublicKey;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.security.GeneralSecurityException;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class SignatureKeyParser implements KeyParser {

	@Override
	public PublicKey parsePublicKey(byte[] encodedKey)
			throws GeneralSecurityException {
		if (encodedKey.length != 32) throw new GeneralSecurityException();
		return new SignaturePublicKey(encodedKey);
	}

	@Override
	public PrivateKey parsePrivateKey(byte[] encodedKey)
			throws GeneralSecurityException {
		if (encodedKey.length != 32) throw new GeneralSecurityException();
		return new SignaturePrivateKey(encodedKey);
	}
}

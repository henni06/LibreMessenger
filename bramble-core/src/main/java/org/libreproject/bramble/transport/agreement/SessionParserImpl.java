package org.libreproject.bramble.transport.agreement;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.crypto.KeyPair;
import org.libreproject.bramble.api.crypto.PrivateKey;
import org.libreproject.bramble.api.crypto.PublicKey;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.api.transport.KeySetId;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.libreproject.bramble.transport.agreement.TransportKeyAgreementConstants.SESSION_KEY_KEY_SET_ID;
import static org.libreproject.bramble.transport.agreement.TransportKeyAgreementConstants.SESSION_KEY_LAST_LOCAL_MESSAGE_ID;
import static org.libreproject.bramble.transport.agreement.TransportKeyAgreementConstants.SESSION_KEY_LOCAL_PRIVATE_KEY;
import static org.libreproject.bramble.transport.agreement.TransportKeyAgreementConstants.SESSION_KEY_LOCAL_PUBLIC_KEY;
import static org.libreproject.bramble.transport.agreement.TransportKeyAgreementConstants.SESSION_KEY_LOCAL_TIMESTAMP;
import static org.libreproject.bramble.transport.agreement.TransportKeyAgreementConstants.SESSION_KEY_STATE;

@Immutable
@NotNullByDefault
class SessionParserImpl implements SessionParser {

	private final TransportKeyAgreementCrypto crypto;

	@Inject
	SessionParserImpl(TransportKeyAgreementCrypto crypto) {
		this.crypto = crypto;
	}

	@Override
	public Session parseSession(BdfDictionary meta) throws FormatException {
		State state =
				State.fromValue(meta.getLong(SESSION_KEY_STATE).intValue());

		MessageId lastLocalMessageId = null;
		byte[] lastLocalMessageIdBytes =
				meta.getOptionalRaw(SESSION_KEY_LAST_LOCAL_MESSAGE_ID);
		if (lastLocalMessageIdBytes != null) {
			lastLocalMessageId = new MessageId(lastLocalMessageIdBytes);
		}

		KeyPair localKeyPair = null;
		byte[] localPublicKeyBytes =
				meta.getOptionalRaw(SESSION_KEY_LOCAL_PUBLIC_KEY);
		byte[] localPrivateKeyBytes =
				meta.getOptionalRaw(SESSION_KEY_LOCAL_PRIVATE_KEY);
		if (localPublicKeyBytes != null && localPrivateKeyBytes != null) {
			PublicKey pub = crypto.parsePublicKey(localPublicKeyBytes);
			PrivateKey priv = crypto.parsePrivateKey(localPrivateKeyBytes);
			localKeyPair = new KeyPair(pub, priv);
		}

		Long localTimestamp = meta.getOptionalLong(SESSION_KEY_LOCAL_TIMESTAMP);

		KeySetId keySetId = null;
		Long keySetIdLong = meta.getOptionalLong(SESSION_KEY_KEY_SET_ID);
		if (keySetIdLong != null) {
			keySetId = new KeySetId(keySetIdLong.intValue());
		}

		return new Session(state, lastLocalMessageId, localKeyPair,
				localTimestamp, keySetId);
	}
}

package org.libreproject.bramble.transport.agreement;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.client.BdfMessageContext;
import org.libreproject.bramble.api.client.BdfMessageValidator;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.sync.Group;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.api.system.Clock;

import javax.annotation.concurrent.Immutable;

import static java.util.Collections.singletonList;
import static org.libreproject.bramble.api.crypto.CryptoConstants.MAX_AGREEMENT_PUBLIC_KEY_BYTES;
import static org.libreproject.bramble.api.plugin.TransportId.MAX_TRANSPORT_ID_LENGTH;
import static org.libreproject.bramble.api.system.Clock.MIN_REASONABLE_TIME_MS;
import static org.libreproject.bramble.transport.agreement.MessageType.ACTIVATE;
import static org.libreproject.bramble.transport.agreement.MessageType.KEY;
import static org.libreproject.bramble.transport.agreement.TransportKeyAgreementConstants.MSG_KEY_PUBLIC_KEY;
import static org.libreproject.bramble.util.ValidationUtils.checkLength;
import static org.libreproject.bramble.util.ValidationUtils.checkSize;

@Immutable
@NotNullByDefault
class TransportKeyAgreementValidator extends BdfMessageValidator {

	private final MessageEncoder messageEncoder;

	TransportKeyAgreementValidator(ClientHelper clientHelper,
			MetadataEncoder metadataEncoder, Clock clock,
			MessageEncoder messageEncoder) {
		super(clientHelper, metadataEncoder, clock);
		this.messageEncoder = messageEncoder;
	}

	@Override
	protected BdfMessageContext validateMessage(Message m, Group g,
			BdfList body) throws FormatException {
		MessageType type = MessageType.fromValue(body.getLong(0).intValue());
		if (type == KEY) return validateKeyMessage(m.getTimestamp(), body);
		else if (type == ACTIVATE) return validateActivateMessage(body);
		else throw new AssertionError();
	}

	private BdfMessageContext validateKeyMessage(long timestamp, BdfList body)
			throws FormatException {
		if (timestamp < MIN_REASONABLE_TIME_MS) throw new FormatException();
		// Message type, transport ID, public key
		checkSize(body, 3);
		String transportId = body.getString(1);
		checkLength(transportId, 1, MAX_TRANSPORT_ID_LENGTH);
		byte[] publicKey = body.getRaw(2);
		checkLength(publicKey, 1, MAX_AGREEMENT_PUBLIC_KEY_BYTES);
		BdfDictionary meta = messageEncoder.encodeMessageMetadata(
				new TransportId(transportId), KEY, false);
		meta.put(MSG_KEY_PUBLIC_KEY, publicKey);
		return new BdfMessageContext(meta);
	}

	private BdfMessageContext validateActivateMessage(BdfList body)
			throws FormatException {
		// Message type, transport ID, previous message ID
		checkSize(body, 3);
		String transportId = body.getString(1);
		checkLength(transportId, 1, MAX_TRANSPORT_ID_LENGTH);
		byte[] previousMessageId = body.getRaw(2);
		checkLength(previousMessageId, MessageId.LENGTH);
		BdfDictionary meta = messageEncoder.encodeMessageMetadata(
				new TransportId(transportId), ACTIVATE, false);
		MessageId dependency = new MessageId(previousMessageId);
		return new BdfMessageContext(meta, singletonList(dependency));
	}
}

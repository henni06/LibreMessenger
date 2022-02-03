package org.libreproject.libre.introduction;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.UniqueId;
import org.libreproject.bramble.api.client.BdfMessageContext;
import org.libreproject.bramble.api.client.BdfMessageValidator;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.Group;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.libre.api.client.SessionId;

import javax.annotation.concurrent.Immutable;

import static java.util.Collections.singletonList;
import static org.libreproject.bramble.api.crypto.CryptoConstants.MAC_BYTES;
import static org.libreproject.bramble.api.crypto.CryptoConstants.MAX_SIGNATURE_BYTES;
import static org.libreproject.bramble.api.identity.AuthorConstants.MAX_PUBLIC_KEY_LENGTH;
import static org.libreproject.bramble.util.ValidationUtils.checkLength;
import static org.libreproject.bramble.util.ValidationUtils.checkSize;
import static org.libreproject.libre.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.libreproject.libre.api.introduction.IntroductionConstants.MAX_INTRODUCTION_TEXT_LENGTH;
import static org.libreproject.libre.introduction.MessageType.ACCEPT;
import static org.libreproject.libre.introduction.MessageType.ACTIVATE;
import static org.libreproject.libre.introduction.MessageType.AUTH;
import static org.libreproject.libre.util.ValidationUtils.validateAutoDeleteTimer;


@Immutable
@NotNullByDefault
class IntroductionValidator extends BdfMessageValidator {

	private final MessageEncoder messageEncoder;

	IntroductionValidator(MessageEncoder messageEncoder,
			ClientHelper clientHelper, MetadataEncoder metadataEncoder,
			Clock clock) {
		super(clientHelper, metadataEncoder, clock);
		this.messageEncoder = messageEncoder;
	}

	@Override
	protected BdfMessageContext validateMessage(Message m, Group g,
			BdfList body) throws FormatException {
		MessageType type = MessageType.fromValue(body.getLong(0).intValue());

		switch (type) {
			case REQUEST:
				return validateRequestMessage(m, body);
			case ACCEPT:
				return validateAcceptMessage(m, body);
			case DECLINE:
				return validateDeclineMessage(type, m, body);
			case AUTH:
				return validateAuthMessage(m, body);
			case ACTIVATE:
				return validateActivateMessage(m, body);
			case ABORT:
				return validateAbortMessage(type, m, body);
			default:
				throw new FormatException();
		}
	}

	private BdfMessageContext validateRequestMessage(Message m, BdfList body)
			throws FormatException {
		// Client version 0.0: Message type, optional previous message ID,
		// author, optional text.
		// Client version 0.1: Message type, optional previous message ID,
		// author, optional text, optional auto-delete timer.
		checkSize(body, 4, 5);

		byte[] previousMessageId = body.getOptionalRaw(1);
		checkLength(previousMessageId, UniqueId.LENGTH);

		BdfList authorList = body.getList(2);
		clientHelper.parseAndValidateAuthor(authorList);

		String text = body.getOptionalString(3);
		checkLength(text, 1, MAX_INTRODUCTION_TEXT_LENGTH);

		long timer = NO_AUTO_DELETE_TIMER;
		if (body.size() == 5) {
			timer = validateAutoDeleteTimer(body.getOptionalLong(4));
		}

		BdfDictionary meta =
				messageEncoder.encodeRequestMetadata(m.getTimestamp(), timer);
		if (previousMessageId == null) {
			return new BdfMessageContext(meta);
		} else {
			MessageId dependency = new MessageId(previousMessageId);
			return new BdfMessageContext(meta, singletonList(dependency));
		}
	}

	private BdfMessageContext validateAcceptMessage(Message m, BdfList body)
			throws FormatException {
		// Client version 0.0: Message type, session ID, optional previous
		// message ID, ephemeral public key, timestamp, transport properties.
		// Client version 0.1: Message type, session ID, optional previous
		// message ID, ephemeral public key, timestamp, transport properties,
		// optional auto-delete timer.
		checkSize(body, 6, 7);

		byte[] sessionIdBytes = body.getRaw(1);
		checkLength(sessionIdBytes, UniqueId.LENGTH);

		byte[] previousMessageId = body.getOptionalRaw(2);
		checkLength(previousMessageId, UniqueId.LENGTH);

		byte[] ephemeralPublicKey = body.getRaw(3);
		checkLength(ephemeralPublicKey, 0, MAX_PUBLIC_KEY_LENGTH);
		clientHelper.parseAndValidateAgreementPublicKey(ephemeralPublicKey);

		long timestamp = body.getLong(4);
		if (timestamp < 0) throw new FormatException();

		BdfDictionary transportProperties = body.getDictionary(5);
		if (transportProperties.size() < 1) throw new FormatException();
		clientHelper
				.parseAndValidateTransportPropertiesMap(transportProperties);

		long timer = NO_AUTO_DELETE_TIMER;
		if (body.size() == 7) {
			timer = validateAutoDeleteTimer(body.getOptionalLong(6));
		}

		SessionId sessionId = new SessionId(sessionIdBytes);
		BdfDictionary meta = messageEncoder.encodeMetadata(ACCEPT, sessionId,
				m.getTimestamp(), timer);
		if (previousMessageId == null) {
			return new BdfMessageContext(meta);
		} else {
			MessageId dependency = new MessageId(previousMessageId);
			return new BdfMessageContext(meta, singletonList(dependency));
		}
	}

	private BdfMessageContext validateDeclineMessage(MessageType type,
			Message m, BdfList body) throws FormatException {
		// Client version 0.0: Message type, session ID, optional previous
		// message ID.
		// Client version 0.1: Message type, session ID, optional previous
		// message ID, optional auto-delete timer.
		checkSize(body, 3, 4);

		byte[] sessionIdBytes = body.getRaw(1);
		checkLength(sessionIdBytes, UniqueId.LENGTH);

		byte[] previousMessageId = body.getOptionalRaw(2);
		checkLength(previousMessageId, UniqueId.LENGTH);

		long timer = NO_AUTO_DELETE_TIMER;
		if (body.size() == 4) {
			timer = validateAutoDeleteTimer(body.getOptionalLong(3));
		}

		SessionId sessionId = new SessionId(sessionIdBytes);
		BdfDictionary meta = messageEncoder.encodeMetadata(type, sessionId,
				m.getTimestamp(), timer);
		if (previousMessageId == null) {
			return new BdfMessageContext(meta);
		} else {
			MessageId dependency = new MessageId(previousMessageId);
			return new BdfMessageContext(meta, singletonList(dependency));
		}
	}

	private BdfMessageContext validateAuthMessage(Message m, BdfList body)
			throws FormatException {
		checkSize(body, 5);

		byte[] sessionIdBytes = body.getRaw(1);
		checkLength(sessionIdBytes, UniqueId.LENGTH);

		byte[] previousMessageId = body.getRaw(2);
		checkLength(previousMessageId, UniqueId.LENGTH);

		byte[] mac = body.getRaw(3);
		checkLength(mac, MAC_BYTES);

		byte[] signature = body.getRaw(4);
		checkLength(signature, 1, MAX_SIGNATURE_BYTES);

		SessionId sessionId = new SessionId(sessionIdBytes);
		BdfDictionary meta = messageEncoder.encodeMetadata(AUTH, sessionId,
				m.getTimestamp(), NO_AUTO_DELETE_TIMER);
		MessageId dependency = new MessageId(previousMessageId);
		return new BdfMessageContext(meta, singletonList(dependency));
	}

	private BdfMessageContext validateActivateMessage(Message m, BdfList body)
			throws FormatException {
		checkSize(body, 4);

		byte[] sessionIdBytes = body.getRaw(1);
		checkLength(sessionIdBytes, UniqueId.LENGTH);

		byte[] previousMessageId = body.getRaw(2);
		checkLength(previousMessageId, UniqueId.LENGTH);

		byte[] mac = body.getOptionalRaw(3);
		checkLength(mac, MAC_BYTES);

		SessionId sessionId = new SessionId(sessionIdBytes);
		BdfDictionary meta = messageEncoder.encodeMetadata(ACTIVATE, sessionId,
				m.getTimestamp(), NO_AUTO_DELETE_TIMER);
		if (previousMessageId == null) {
			return new BdfMessageContext(meta);
		} else {
			MessageId dependency = new MessageId(previousMessageId);
			return new BdfMessageContext(meta, singletonList(dependency));
		}
	}

	private BdfMessageContext validateAbortMessage(MessageType type,
			Message m, BdfList body) throws FormatException {
		checkSize(body, 3);

		byte[] sessionIdBytes = body.getRaw(1);
		checkLength(sessionIdBytes, UniqueId.LENGTH);

		byte[] previousMessageId = body.getOptionalRaw(2);
		checkLength(previousMessageId, UniqueId.LENGTH);

		SessionId sessionId = new SessionId(sessionIdBytes);
		BdfDictionary meta = messageEncoder.encodeMetadata(type, sessionId,
				m.getTimestamp(), NO_AUTO_DELETE_TIMER);
		if (previousMessageId == null) {
			return new BdfMessageContext(meta);
		} else {
			MessageId dependency = new MessageId(previousMessageId);
			return new BdfMessageContext(meta, singletonList(dependency));
		}
	}
}

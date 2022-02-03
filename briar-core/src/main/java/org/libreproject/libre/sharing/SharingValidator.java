package org.libreproject.libre.sharing;

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
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.api.system.Clock;

import javax.annotation.concurrent.Immutable;

import static java.util.Collections.singletonList;
import static org.libreproject.bramble.util.ValidationUtils.checkLength;
import static org.libreproject.bramble.util.ValidationUtils.checkSize;
import static org.libreproject.libre.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.libreproject.libre.api.sharing.SharingConstants.MAX_INVITATION_TEXT_LENGTH;
import static org.libreproject.libre.sharing.MessageType.INVITE;
import static org.libreproject.libre.util.ValidationUtils.validateAutoDeleteTimer;

@Immutable
@NotNullByDefault
abstract class SharingValidator extends BdfMessageValidator {

	private final MessageEncoder messageEncoder;

	SharingValidator(MessageEncoder messageEncoder, ClientHelper clientHelper,
			MetadataEncoder metadataEncoder, Clock clock) {
		super(clientHelper, metadataEncoder, clock);
		this.messageEncoder = messageEncoder;
	}

	@Override
	protected BdfMessageContext validateMessage(Message m, Group g,
			BdfList body) throws FormatException {
		MessageType type = MessageType.fromValue(body.getLong(0).intValue());
		switch (type) {
			case INVITE:
				return validateInviteMessage(m, body);
			case ACCEPT:
			case DECLINE:
				return validateAcceptOrDeclineMessage(type, m, body);
			case LEAVE:
			case ABORT:
				return validateLeaveOrAbortMessage(type, m, body);
			default:
				throw new FormatException();
		}
	}

	private BdfMessageContext validateInviteMessage(Message m, BdfList body)
			throws FormatException {
		// Client version 0.0: Message type, optional previous message ID,
		// descriptor, optional text.
		// Client version 0.1: Message type, optional previous message ID,
		// descriptor, optional text, optional auto-delete timer.
		checkSize(body, 4, 5);
		byte[] previousMessageId = body.getOptionalRaw(1);
		checkLength(previousMessageId, UniqueId.LENGTH);
		BdfList descriptor = body.getList(2);
		GroupId shareableId = validateDescriptor(descriptor);
		String text = body.getOptionalString(3);
		checkLength(text, 1, MAX_INVITATION_TEXT_LENGTH);
		long timer = NO_AUTO_DELETE_TIMER;
		if (body.size() == 5) {
			timer = validateAutoDeleteTimer(body.getOptionalLong(4));
		}

		BdfDictionary meta = messageEncoder.encodeMetadata(INVITE, shareableId,
				m.getTimestamp(), false, false, false, false, false, timer,
				false);
		if (previousMessageId == null) {
			return new BdfMessageContext(meta);
		} else {
			MessageId dependency = new MessageId(previousMessageId);
			return new BdfMessageContext(meta, singletonList(dependency));
		}
	}

	protected abstract GroupId validateDescriptor(BdfList descriptor)
			throws FormatException;

	private BdfMessageContext validateLeaveOrAbortMessage(MessageType type,
			Message m, BdfList body) throws FormatException {
		checkSize(body, 3);
		byte[] shareableId = body.getRaw(1);
		checkLength(shareableId, UniqueId.LENGTH);
		byte[] previousMessageId = body.getOptionalRaw(2);
		checkLength(previousMessageId, UniqueId.LENGTH);

		BdfDictionary meta = messageEncoder.encodeMetadata(type,
				new GroupId(shareableId), m.getTimestamp(), false, false,
				false, false, false, NO_AUTO_DELETE_TIMER, false);
		if (previousMessageId == null) {
			return new BdfMessageContext(meta);
		} else {
			MessageId dependency = new MessageId(previousMessageId);
			return new BdfMessageContext(meta, singletonList(dependency));
		}
	}

	private BdfMessageContext validateAcceptOrDeclineMessage(MessageType type,
			Message m, BdfList body) throws FormatException {
		// Client version 0.0: Message type, shareable ID, optional previous
		// message ID.
		// Client version 0.1: Message type, shareable ID, optional previous
		// message ID, optional auto-delete timer.
		checkSize(body, 3, 4);
		byte[] shareableId = body.getRaw(1);
		checkLength(shareableId, UniqueId.LENGTH);
		byte[] previousMessageId = body.getOptionalRaw(2);
		checkLength(previousMessageId, UniqueId.LENGTH);
		long timer = NO_AUTO_DELETE_TIMER;
		if (body.size() == 4) {
			timer = validateAutoDeleteTimer(body.getOptionalLong(3));
		}

		BdfDictionary meta = messageEncoder.encodeMetadata(type,
				new GroupId(shareableId), m.getTimestamp(), false, false,
				false, false, false, timer, false);
		if (previousMessageId == null) {
			return new BdfMessageContext(meta);
		} else {
			MessageId dependency = new MessageId(previousMessageId);
			return new BdfMessageContext(meta, singletonList(dependency));
		}
	}
}

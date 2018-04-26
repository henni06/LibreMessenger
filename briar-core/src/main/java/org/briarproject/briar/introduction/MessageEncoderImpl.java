package org.briarproject.briar.introduction;

import org.briarproject.bramble.api.FormatException;
import org.briarproject.bramble.api.client.ClientHelper;
import org.briarproject.bramble.api.data.BdfDictionary;
import org.briarproject.bramble.api.data.BdfList;
import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.properties.TransportProperties;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.Message;
import org.briarproject.bramble.api.sync.MessageFactory;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.briar.api.client.SessionId;

import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static org.briarproject.briar.client.MessageTrackerConstants.MSG_KEY_READ;
import static org.briarproject.briar.introduction.IntroductionConstants.MSG_KEY_AVAILABLE_TO_ANSWER;
import static org.briarproject.briar.introduction.IntroductionConstants.MSG_KEY_LOCAL;
import static org.briarproject.briar.introduction.IntroductionConstants.MSG_KEY_MESSAGE_TYPE;
import static org.briarproject.briar.introduction.IntroductionConstants.MSG_KEY_SESSION_ID;
import static org.briarproject.briar.introduction.IntroductionConstants.MSG_KEY_TIMESTAMP;
import static org.briarproject.briar.introduction.IntroductionConstants.MSG_KEY_VISIBLE_IN_UI;
import static org.briarproject.briar.introduction.MessageType.ABORT;
import static org.briarproject.briar.introduction.MessageType.ACCEPT;
import static org.briarproject.briar.introduction.MessageType.ACTIVATE;
import static org.briarproject.briar.introduction.MessageType.AUTH;
import static org.briarproject.briar.introduction.MessageType.DECLINE;
import static org.briarproject.briar.introduction.MessageType.REQUEST;

@NotNullByDefault
class MessageEncoderImpl implements MessageEncoder {

	private final ClientHelper clientHelper;
	private final MessageFactory messageFactory;

	@Inject
	MessageEncoderImpl(ClientHelper clientHelper,
			MessageFactory messageFactory) {
		this.clientHelper = clientHelper;
		this.messageFactory = messageFactory;
	}

	@Override
	public BdfDictionary encodeRequestMetadata(long timestamp) {
		BdfDictionary meta =
				encodeMetadata(REQUEST, null, timestamp, false, false, false);
		meta.put(MSG_KEY_AVAILABLE_TO_ANSWER, false);
		return meta;
	}

	@Override
	public BdfDictionary encodeMetadata(MessageType type,
			@Nullable SessionId sessionId, long timestamp, boolean local,
			boolean read, boolean visible) {
		BdfDictionary meta = new BdfDictionary();
		meta.put(MSG_KEY_MESSAGE_TYPE, type.getValue());
		if (sessionId != null)
			meta.put(MSG_KEY_SESSION_ID, sessionId);
		else if (type != REQUEST)
			throw new IllegalArgumentException();
		meta.put(MSG_KEY_TIMESTAMP, timestamp);
		meta.put(MSG_KEY_LOCAL, local);
		meta.put(MSG_KEY_READ, read);
		meta.put(MSG_KEY_VISIBLE_IN_UI, visible);
		return meta;
	}

	@Override
	public void addSessionId(BdfDictionary meta, SessionId sessionId) {
		meta.put(MSG_KEY_SESSION_ID, sessionId);
	}

	@Override
	public void setVisibleInUi(BdfDictionary meta, boolean visible) {
		meta.put(MSG_KEY_VISIBLE_IN_UI, visible);
	}

	@Override
	public void setAvailableToAnswer(BdfDictionary meta, boolean available) {
		meta.put(MSG_KEY_AVAILABLE_TO_ANSWER, available);
	}

	@Override
	public Message encodeRequestMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, Author author,
			@Nullable String message) {
		if (message != null && message.equals("")) {
			throw new IllegalArgumentException();
		}
		BdfList body = BdfList.of(
				REQUEST.getValue(),
				previousMessageId,
				clientHelper.toList(author),
				message
		);
		return createMessage(contactGroupId, timestamp, body);
	}

	@Override
	public Message encodeAcceptMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, SessionId sessionId,
			byte[] ephemeralPublicKey, long acceptTimestamp,
			Map<TransportId, TransportProperties> transportProperties) {
		BdfList body = BdfList.of(
				ACCEPT.getValue(),
				sessionId,
				previousMessageId,
				ephemeralPublicKey,
				acceptTimestamp,
				clientHelper.toDictionary(transportProperties)
		);
		return createMessage(contactGroupId, timestamp, body);
	}

	@Override
	public Message encodeDeclineMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, SessionId sessionId) {
		return encodeMessage(DECLINE, contactGroupId, sessionId, timestamp,
				previousMessageId);
	}

	@Override
	public Message encodeAuthMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, SessionId sessionId,
			byte[] mac, byte[] signature) {
		BdfList body = BdfList.of(
				AUTH.getValue(),
				sessionId,
				previousMessageId,
				mac,
				signature
		);
		return createMessage(contactGroupId, timestamp, body);
	}

	@Override
	public Message encodeActivateMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, SessionId sessionId,
			byte[] mac) {
		BdfList body = BdfList.of(
				ACTIVATE.getValue(),
				sessionId,
				previousMessageId,
				mac
		);
		return createMessage(contactGroupId, timestamp, body);
	}

	@Override
	public Message encodeAbortMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, SessionId sessionId) {
		return encodeMessage(ABORT, contactGroupId, sessionId, timestamp,
				previousMessageId);
	}

	private Message encodeMessage(MessageType type, GroupId contactGroupId,
			SessionId sessionId, long timestamp,
			@Nullable MessageId previousMessageId) {
		BdfList body = BdfList.of(
				type.getValue(),
				sessionId,
				previousMessageId
		);
		return createMessage(contactGroupId, timestamp, body);
	}

	private Message createMessage(GroupId contactGroupId, long timestamp,
			BdfList body) {
		try {
			return messageFactory.createMessage(contactGroupId, timestamp,
					clientHelper.toByteArray(body));
		} catch (FormatException e) {
			throw new AssertionError(e);
		}
	}

}

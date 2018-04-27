package org.briarproject.briar.introduction;

import org.briarproject.bramble.api.FormatException;
import org.briarproject.bramble.api.client.ClientHelper;
import org.briarproject.bramble.api.client.ContactGroupFactory;
import org.briarproject.bramble.api.contact.Contact;
import org.briarproject.bramble.api.contact.ContactManager;
import org.briarproject.bramble.api.data.BdfDictionary;
import org.briarproject.bramble.api.db.DatabaseComponent;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.Transaction;
import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.identity.AuthorId;
import org.briarproject.bramble.api.identity.IdentityManager;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.properties.TransportProperties;
import org.briarproject.bramble.api.sync.Message;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.bramble.api.system.Clock;
import org.briarproject.briar.api.client.MessageTracker;
import org.briarproject.briar.api.client.SessionId;
import org.briarproject.briar.api.introduction.IntroductionResponse;
import org.briarproject.briar.api.introduction.event.IntroductionResponseReceivedEvent;

import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.briarproject.briar.introduction.MessageType.ABORT;
import static org.briarproject.briar.introduction.MessageType.ACCEPT;
import static org.briarproject.briar.introduction.MessageType.ACTIVATE;
import static org.briarproject.briar.introduction.MessageType.AUTH;
import static org.briarproject.briar.introduction.MessageType.DECLINE;
import static org.briarproject.briar.introduction.MessageType.REQUEST;

@Immutable
@NotNullByDefault
abstract class AbstractProtocolEngine<S extends Session>
		implements ProtocolEngine<S> {

	protected final DatabaseComponent db;
	protected final ClientHelper clientHelper;
	protected final ContactManager contactManager;
	protected final ContactGroupFactory contactGroupFactory;
	protected final MessageTracker messageTracker;
	protected final IdentityManager identityManager;
	protected final MessageParser messageParser;
	protected final MessageEncoder messageEncoder;
	protected final Clock clock;

	AbstractProtocolEngine(
			DatabaseComponent db,
			ClientHelper clientHelper,
			ContactManager contactManager,
			ContactGroupFactory contactGroupFactory,
			MessageTracker messageTracker,
			IdentityManager identityManager,
			MessageParser messageParser,
			MessageEncoder messageEncoder,
			Clock clock) {
		this.db = db;
		this.clientHelper = clientHelper;
		this.contactManager = contactManager;
		this.contactGroupFactory = contactGroupFactory;
		this.messageTracker = messageTracker;
		this.identityManager = identityManager;
		this.messageParser = messageParser;
		this.messageEncoder = messageEncoder;
		this.clock = clock;
	}

	Message sendRequestMessage(Transaction txn, PeerSession s,
			long timestamp, Author author, @Nullable String message)
			throws DbException {
		Message m = messageEncoder
				.encodeRequestMessage(s.getContactGroupId(), timestamp,
						s.getLastLocalMessageId(), author, message);
		sendMessage(txn, REQUEST, s.getSessionId(), m, true);
		return m;
	}

	Message sendAcceptMessage(Transaction txn, PeerSession s, long timestamp,
			byte[] ephemeralPublicKey, long acceptTimestamp,
			Map<TransportId, TransportProperties> transportProperties,
			boolean visible) throws DbException {
		Message m = messageEncoder
				.encodeAcceptMessage(s.getContactGroupId(), timestamp,
						s.getLastLocalMessageId(), s.getSessionId(),
						ephemeralPublicKey, acceptTimestamp,
						transportProperties);
		sendMessage(txn, ACCEPT, s.getSessionId(), m, visible);
		return m;
	}

	Message sendDeclineMessage(Transaction txn, PeerSession s, long timestamp,
			boolean visible) throws DbException {
		Message m = messageEncoder
				.encodeDeclineMessage(s.getContactGroupId(), timestamp,
						s.getLastLocalMessageId(), s.getSessionId());
		sendMessage(txn, DECLINE, s.getSessionId(), m, visible);
		return m;
	}

	Message sendAuthMessage(Transaction txn, PeerSession s, long timestamp,
			byte[] mac, byte[] signature) throws DbException {
		Message m = messageEncoder
				.encodeAuthMessage(s.getContactGroupId(), timestamp,
						s.getLastLocalMessageId(), s.getSessionId(), mac,
						signature);
		sendMessage(txn, AUTH, s.getSessionId(), m, false);
		return m;
	}

	Message sendActivateMessage(Transaction txn, PeerSession s, long timestamp,
			byte[] mac) throws DbException {
		Message m = messageEncoder
				.encodeActivateMessage(s.getContactGroupId(), timestamp,
						s.getLastLocalMessageId(), s.getSessionId(), mac);
		sendMessage(txn, ACTIVATE, s.getSessionId(), m, false);
		return m;
	}

	Message sendAbortMessage(Transaction txn, PeerSession s, long timestamp)
			throws DbException {
		Message m = messageEncoder
				.encodeAbortMessage(s.getContactGroupId(), timestamp,
						s.getLastLocalMessageId(), s.getSessionId());
		sendMessage(txn, ABORT, s.getSessionId(), m, false);
		return m;
	}

	private void sendMessage(Transaction txn, MessageType type,
			SessionId sessionId, Message m, boolean visibleInConversation)
			throws DbException {
		BdfDictionary meta = messageEncoder
				.encodeMetadata(type, sessionId, m.getTimestamp(), true, true,
						visibleInConversation);
		try {
			clientHelper.addLocalMessage(txn, m, meta, true);
		} catch (FormatException e) {
			throw new AssertionError(e);
		}
	}

	void broadcastIntroductionResponseReceivedEvent(Transaction txn, Session s,
			AuthorId sender, Author otherAuthor, AbstractIntroductionMessage m)
			throws DbException {
		AuthorId localAuthorId = identityManager.getLocalAuthor(txn).getId();
		Contact c = contactManager.getContact(txn, sender, localAuthorId);
		IntroductionResponse response =
				new IntroductionResponse(s.getSessionId(), m.getMessageId(),
						m.getGroupId(), s.getRole(), m.getTimestamp(), false,
						false, false, false, otherAuthor.getName(),
						m instanceof AcceptMessage);
		IntroductionResponseReceivedEvent e =
				new IntroductionResponseReceivedEvent(c.getId(), response);
		txn.attach(e);
	}

	void markMessageVisibleInUi(Transaction txn, MessageId m)
			throws DbException {
		BdfDictionary meta = new BdfDictionary();
		messageEncoder.setVisibleInUi(meta, true);
		try {
			clientHelper.mergeMessageMetadata(txn, m, meta);
		} catch (FormatException e) {
			throw new AssertionError(e);
		}
	}

	boolean isInvalidDependency(@Nullable MessageId lastRemoteMessageId,
			@Nullable MessageId dependency) {
		if (dependency == null) return lastRemoteMessageId != null;
		return lastRemoteMessageId == null ||
				!dependency.equals(lastRemoteMessageId);
	}

	long getLocalTimestamp(long localTimestamp, long requestTimestamp) {
		return Math.max(
				clock.currentTimeMillis(),
				Math.max(
						localTimestamp,
						requestTimestamp
				) + 1
		);
	}

}

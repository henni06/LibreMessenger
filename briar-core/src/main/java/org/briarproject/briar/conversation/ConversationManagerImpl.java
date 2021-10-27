package org.briarproject.briar.conversation;

import org.briarproject.bramble.api.client.ClientHelper;
import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.db.DatabaseComponent;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.Transaction;
import org.briarproject.bramble.api.event.Event;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.Message;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.bramble.api.system.Clock;
import org.briarproject.briar.api.client.MessageTracker;
import org.briarproject.briar.api.client.MessageTracker.GroupCount;
import org.briarproject.briar.api.conversation.ConversationManager;
import org.briarproject.briar.api.conversation.ConversationMessageHeader;
import org.briarproject.briar.api.conversation.DeletionResult;
import org.briarproject.briar.api.conversation.event.ConversationMessageTrackedEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import static java.lang.Math.max;

@ThreadSafe
@NotNullByDefault
class ConversationManagerImpl implements ConversationManager {

	private final DatabaseComponent db;
	private final MessageTracker messageTracker;
	private final Clock clock;
	private final ClientHelper clientHelper;
	private final Set<ConversationClient> clients;

	@Inject
	ConversationManagerImpl(DatabaseComponent db, MessageTracker messageTracker,
			Clock clock, ClientHelper clientHelper) {
		this.db = db;
		this.messageTracker = messageTracker;
		this.clock = clock;
		this.clientHelper = clientHelper;
		clients = new CopyOnWriteArraySet<>();
	}

	@Override
	public void registerConversationClient(ConversationClient client) {
		if (!clients.add(client))
			throw new IllegalStateException("Client is already registered");
	}

	@Override
	public Collection<ConversationMessageHeader> getMessageHeaders(ContactId c)
			throws DbException {
		List<ConversationMessageHeader> messages = new ArrayList<>();
		Transaction txn = db.startTransaction(true);
		try {
			for (ConversationClient client : clients) {
				messages.addAll(client.getMessageHeaders(txn, c));
			}
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return messages;
	}

	@Override
	public GroupCount getGroupCount(ContactId contactId) throws DbException {
		return db.transactionWithResult(true,
				txn -> getGroupCount(txn, contactId));
	}

	@Override
	public GroupCount getGroupCount(Transaction txn, ContactId contactId)
			throws DbException {
		int msgCount = 0, unreadCount = 0;
		long latestTime = 0;
		for (ConversationClient client : clients) {
			GroupCount count = client.getGroupCount(txn, contactId);
			msgCount += count.getMsgCount();
			unreadCount += count.getUnreadCount();
			if (count.getLatestMsgTime() > latestTime)
				latestTime = count.getLatestMsgTime();
		}
		return new GroupCount(msgCount, unreadCount, latestTime);
	}

	@Override
	public void trackIncomingMessage(Transaction txn, Message m)
			throws DbException {
		messageTracker.trackIncomingMessage(txn, m);
		Event e = new ConversationMessageTrackedEvent(
				m.getTimestamp(), false,
				clientHelper.getContactId(txn, m.getGroupId()));
		txn.attach(e);
	}

	@Override
	public void trackOutgoingMessage(Transaction txn, Message m)
			throws DbException {
		messageTracker.trackOutgoingMessage(txn, m);
		Event e = new ConversationMessageTrackedEvent(
				m.getTimestamp(), true,
				clientHelper.getContactId(txn, m.getGroupId()));
		txn.attach(e);
	}

	@Override
	public void trackMessage(Transaction txn, GroupId g, long timestamp,
			boolean read)
			throws DbException {
		messageTracker.trackMessage(txn, g, timestamp, read);
		Event e = new ConversationMessageTrackedEvent(
				timestamp, read, clientHelper.getContactId(txn, g));
		txn.attach(e);
	}

	@Override
	public void setReadFlag(GroupId g, MessageId m, boolean read)
			throws DbException {
		db.transaction(false, txn -> {
			boolean wasRead = messageTracker.setReadFlag(txn, g, m, read);
			if (read && !wasRead) db.startCleanupTimer(txn, m);
		});
	}

	@Override
	public long getTimestampForOutgoingMessage(Transaction txn, ContactId c)
			throws DbException {
		long now = clock.currentTimeMillis();
		GroupCount gc = getGroupCount(txn, c);
		return max(now, gc.getLatestMsgTime() + 1);
	}

	@Override
	public DeletionResult deleteAllMessages(ContactId c) throws DbException {
		return db.transactionWithResult(false, txn -> {
			DeletionResult result = new DeletionResult();
			for (ConversationClient client : clients) {
				result.addDeletionResult(client.deleteAllMessages(txn, c));
			}
			return result;
		});
	}

	@Override
	public DeletionResult deleteMessages(ContactId c,
			Collection<MessageId> toDelete) throws DbException {
		return db.transactionWithResult(false, txn -> {
			DeletionResult result = new DeletionResult();
			for (ConversationClient client : clients) {
				Set<MessageId> idSet = client.getMessageIds(txn, c);
				idSet.retainAll(toDelete);
				result.addDeletionResult(client.deleteMessages(txn, c, idSet));
			}
			return result;
		});
	}

}

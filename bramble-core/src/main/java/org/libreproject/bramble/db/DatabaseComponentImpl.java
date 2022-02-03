package org.libreproject.bramble.db;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.cleanup.event.CleanupTimerStartedEvent;
import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.contact.PendingContact;
import org.libreproject.bramble.api.contact.PendingContactId;
import org.libreproject.bramble.api.contact.event.ContactAddedEvent;
import org.libreproject.bramble.api.contact.event.ContactAliasChangedEvent;
import org.libreproject.bramble.api.contact.event.ContactRemovedEvent;
import org.libreproject.bramble.api.contact.event.ContactVerifiedEvent;
import org.libreproject.bramble.api.contact.event.PendingContactAddedEvent;
import org.libreproject.bramble.api.contact.event.PendingContactRemovedEvent;
import org.libreproject.bramble.api.crypto.PrivateKey;
import org.libreproject.bramble.api.crypto.PublicKey;
import org.libreproject.bramble.api.crypto.SecretKey;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.data.BdfReader;
import org.libreproject.bramble.api.db.CommitAction;
import org.libreproject.bramble.api.db.CommitAction.Visitor;
import org.libreproject.bramble.api.db.ContactExistsException;
import org.libreproject.bramble.api.db.DatabaseComponent;
import org.libreproject.bramble.api.db.DbCallable;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.DbRunnable;
import org.libreproject.bramble.api.db.EventAction;
import org.libreproject.bramble.api.db.Metadata;
import org.libreproject.bramble.api.db.MigrationListener;
import org.libreproject.bramble.api.db.NoSuchContactException;
import org.libreproject.bramble.api.db.NoSuchGroupException;
import org.libreproject.bramble.api.db.NoSuchIdentityException;
import org.libreproject.bramble.api.db.NoSuchMessageException;
import org.libreproject.bramble.api.db.NoSuchPendingContactException;
import org.libreproject.bramble.api.db.NoSuchTransportException;
import org.libreproject.bramble.api.db.NullableDbCallable;
import org.libreproject.bramble.api.db.PendingContactExistsException;
import org.libreproject.bramble.api.db.TaskAction;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.event.EventExecutor;
import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.identity.AuthorId;
import org.libreproject.bramble.api.identity.Identity;
import org.libreproject.bramble.api.identity.event.IdentityAddedEvent;
import org.libreproject.bramble.api.identity.event.IdentityRemovedEvent;
import org.libreproject.bramble.api.lifecycle.ShutdownManager;
import org.libreproject.bramble.api.location.event.MarkerAddedEvent;
import org.libreproject.bramble.api.location.event.MarkerRemovedEvent;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.settings.Settings;
import org.libreproject.bramble.api.settings.event.SettingsUpdatedEvent;
import org.libreproject.bramble.api.sync.Ack;
import org.libreproject.bramble.api.sync.ClientId;
import org.libreproject.bramble.api.sync.Group;
import org.libreproject.bramble.api.sync.Group.Visibility;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.api.sync.MessageStatus;
import org.libreproject.bramble.api.sync.Offer;
import org.libreproject.bramble.api.sync.Request;
import org.libreproject.bramble.api.sync.event.GroupAddedEvent;
import org.libreproject.bramble.api.sync.event.GroupRemovedEvent;
import org.libreproject.bramble.api.sync.event.GroupVisibilityUpdatedEvent;
import org.libreproject.bramble.api.sync.event.LocationMessageEvent;
import org.libreproject.bramble.api.sync.event.MessageAddedEvent;
import org.libreproject.bramble.api.sync.event.MessageRequestedEvent;
import org.libreproject.bramble.api.sync.event.MessageSharedEvent;
import org.libreproject.bramble.api.sync.event.MessageStateChangedEvent;
import org.libreproject.bramble.api.sync.event.MessageToAckEvent;
import org.libreproject.bramble.api.sync.event.MessageToRequestEvent;
import org.libreproject.bramble.api.sync.event.MessagesAckedEvent;
import org.libreproject.bramble.api.sync.event.MessagesSentEvent;
import org.libreproject.bramble.api.sync.event.SyncVersionsUpdatedEvent;
import org.libreproject.bramble.api.sync.validation.MessageState;
import org.libreproject.bramble.api.transport.KeySetId;
import org.libreproject.bramble.api.transport.TransportKeySet;
import org.libreproject.bramble.api.transport.TransportKeys;
import org.libreproject.bramble.data.BdfReaderFactoryImpl;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.libreproject.bramble.api.sync.Group.Visibility.INVISIBLE;
import static org.libreproject.bramble.api.sync.Group.Visibility.SHARED;
import static org.libreproject.bramble.api.sync.validation.MessageState.DELIVERED;
import static org.libreproject.bramble.api.sync.validation.MessageState.UNKNOWN;
import static org.libreproject.bramble.db.DatabaseConstants.MAX_OFFERED_MESSAGES;
import static org.libreproject.bramble.util.LogUtils.logDuration;
import static org.libreproject.bramble.util.LogUtils.logException;
import static org.libreproject.bramble.util.LogUtils.now;

@ThreadSafe
@NotNullByDefault
class DatabaseComponentImpl<T> implements DatabaseComponent {

	private static final Logger LOG =
			getLogger(DatabaseComponentImpl.class.getName());

	private final Database<T> db;
	private final Class<T> txnClass;
	private final EventBus eventBus;
	private final Executor eventExecutor;
	private final ShutdownManager shutdownManager;
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private final ReentrantReadWriteLock lock =
			new ReentrantReadWriteLock(true);
	private final Visitor visitor = new CommitActionVisitor();

	@Inject
	DatabaseComponentImpl(Database<T> db, Class<T> txnClass, EventBus eventBus,
			@EventExecutor Executor eventExecutor,
			ShutdownManager shutdownManager) {
		this.db = db;
		this.txnClass = txnClass;
		this.eventBus = eventBus;
		this.eventExecutor = eventExecutor;
		this.shutdownManager = shutdownManager;
	}

	@Override
	public boolean open(SecretKey key, @Nullable MigrationListener listener)
			throws DbException {
		boolean reopened = db.open(key, listener);
		shutdownManager.addShutdownHook(() -> {
			try {
				close();
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
		return reopened;
	}

	@Override
	public void close() throws DbException {
		if (closed.getAndSet(true)) return;
		db.close();
	}

	@Override
	public Transaction startTransaction(boolean readOnly) throws DbException {
		// Don't allow reentrant locking
		if (lock.getReadHoldCount() > 0) throw new IllegalStateException();
		if (lock.getWriteHoldCount() > 0) throw new IllegalStateException();
		long start = now();
		if (readOnly) {
			lock.readLock().lock();
			logDuration(LOG, "Waiting for read lock", start);
		} else {
			lock.writeLock().lock();
			logDuration(LOG, "Waiting for write lock", start);
		}
		try {
			return new Transaction(db.startTransaction(), readOnly);
		} catch (DbException | RuntimeException e) {
			if (readOnly) lock.readLock().unlock();
			else lock.writeLock().unlock();
			throw e;
		}
	}

	@Override
	public void commitTransaction(Transaction transaction) throws DbException {
		T txn = txnClass.cast(transaction.unbox());
		if (transaction.isCommitted()) throw new IllegalStateException();
		transaction.setCommitted();
		db.commitTransaction(txn);
	}

	@Override
	public void endTransaction(Transaction transaction) {
		try {
			T txn = txnClass.cast(transaction.unbox());
			if (transaction.isCommitted()) {
				for (CommitAction a : transaction.getActions())
					a.accept(visitor);
			} else {
				db.abortTransaction(txn);
			}
		} finally {
			if (transaction.isReadOnly()) lock.readLock().unlock();
			else lock.writeLock().unlock();
		}
	}

	@Override
	public <E extends Exception> void transaction(boolean readOnly,
			DbRunnable<E> task) throws DbException, E {
		Transaction txn = startTransaction(readOnly);
		try {
			task.run(txn);
			commitTransaction(txn);
		} finally {
			endTransaction(txn);
		}
	}

	@Override
	public <R, E extends Exception> R transactionWithResult(boolean readOnly,
			DbCallable<R, E> task) throws DbException, E {
		Transaction txn = startTransaction(readOnly);
		try {
			R result = task.call(txn);
			commitTransaction(txn);
			return result;
		} finally {
			endTransaction(txn);
		}
	}

	@Override
	public <R, E extends Exception> R transactionWithNullableResult(
			boolean readOnly, NullableDbCallable<R, E> task)
			throws DbException, E {
		Transaction txn = startTransaction(readOnly);
		try {
			R result = task.call(txn);
			commitTransaction(txn);
			return result;
		} finally {
			endTransaction(txn);
		}
	}

	private T unbox(Transaction transaction) {
		if (transaction.isCommitted()) throw new IllegalStateException();
		return txnClass.cast(transaction.unbox());
	}

	@Override
	public ContactId addContact(Transaction transaction, Author remote,
			AuthorId local, @Nullable PublicKey handshake, boolean verified)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsIdentity(txn, local))
			throw new NoSuchIdentityException();
		if (db.containsIdentity(txn, remote.getId()))
			throw new ContactExistsException(local, remote);
		if (db.containsContact(txn, remote.getId(), local))
			throw new ContactExistsException(local, remote);
		ContactId c = db.addContact(txn, remote, local, handshake, verified);
		transaction.attach(new ContactAddedEvent(c, verified));
		return c;
	}

	@Override
	public void addGroup(Transaction transaction, Group g) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, g.getId())) {
			db.addGroup(txn, g);
			transaction.attach(new GroupAddedEvent(g));
		}
	}

	@Override
	public void addIdentity(Transaction transaction, Identity i)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsIdentity(txn, i.getId())) {
			db.addIdentity(txn, i);
			transaction.attach(new IdentityAddedEvent(i.getId()));
		}
	}

	@Override
	public void addLocalMessage(Transaction transaction, Message m,
			Metadata meta, boolean shared, boolean temporary,
			Message.MessageType messageType)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, m.getGroupId()))
			throw new NoSuchGroupException();
		if (!db.containsMessage(txn, m.getId())) {
			db.addMessage(txn, m, DELIVERED, shared, temporary, null,messageType);
			transaction.attach(new MessageAddedEvent(m, null));
			transaction.attach(new MessageStateChangedEvent(m.getId(), true,
					DELIVERED));
			if (shared) transaction.attach(new MessageSharedEvent(m.getId()));
		}
		db.mergeMessageMetadata(txn, m.getId(), meta);
	}

	@Override
	public void addPendingContact(Transaction transaction, PendingContact p,
			AuthorId local) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		Contact contact = db.getContact(txn, p.getPublicKey(), local);
		if (contact != null)
			throw new ContactExistsException(local, contact.getAuthor());
		if (db.containsPendingContact(txn, p.getId())) {
			PendingContact existing = db.getPendingContact(txn, p.getId());
			throw new PendingContactExistsException(existing);
		}
		db.addPendingContact(txn, p);
		transaction.attach(new PendingContactAddedEvent(p));
	}

	@Override
	public void addTransport(Transaction transaction, TransportId t,
			long maxLatency) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsTransport(txn, t))
			db.addTransport(txn, t, maxLatency);
	}

	@Override
	public KeySetId addTransportKeys(Transaction transaction, ContactId c,
			TransportKeys k) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		if (!db.containsTransport(txn, k.getTransportId()))
			throw new NoSuchTransportException();
		return db.addTransportKeys(txn, c, k);
	}

	@Override
	public KeySetId addTransportKeys(Transaction transaction,
			PendingContactId p, TransportKeys k) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsPendingContact(txn, p))
			throw new NoSuchPendingContactException();
		if (!db.containsTransport(txn, k.getTransportId()))
			throw new NoSuchTransportException();
		return db.addTransportKeys(txn, p, k);
	}

	@Override
	public boolean containsAnythingToSend(Transaction transaction, ContactId c,
			long maxLatency, boolean eager) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		return db.containsAnythingToSend(txn, c, maxLatency, eager);
	}

	@Override
	public boolean containsContact(Transaction transaction, AuthorId remote,
			AuthorId local) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsIdentity(txn, local))
			throw new NoSuchIdentityException();
		return db.containsContact(txn, remote, local);
	}

	@Override
	public boolean containsGroup(Transaction transaction, GroupId g)
			throws DbException {
		T txn = unbox(transaction);
		return db.containsGroup(txn, g);
	}

	@Override
	public boolean containsIdentity(Transaction transaction, AuthorId a)
			throws DbException {
		T txn = unbox(transaction);
		return db.containsIdentity(txn, a);
	}

	@Override
	public boolean containsPendingContact(Transaction transaction,
			PendingContactId p) throws DbException {
		T txn = unbox(transaction);
		return db.containsPendingContact(txn, p);
	}

	@Override
	public boolean containsTransportKeys(Transaction transaction, ContactId c,
			TransportId t) throws DbException {
		T txn = unbox(transaction);
		return db.containsTransportKeys(txn, c, t);
	}

	@Override
	public void deleteMessage(Transaction transaction, MessageId m)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		db.deleteMessage(txn, m);
	}

	@Override
	public void deleteMessageMetadata(Transaction transaction, MessageId m)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		db.deleteMessageMetadata(txn, m);
	}

	@Nullable
	@Override
	public Ack generateAck(Transaction transaction, ContactId c,
			int maxMessages) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		Collection<MessageId> ids = db.getMessagesToAck(txn, c, maxMessages);
		if (ids.isEmpty()) return null;
		db.lowerAckFlag(txn, c, ids);
		return new Ack(ids);
	}

	@Nullable
	@Override
	public Collection<Message> generateBatch(Transaction transaction,
			ContactId c, int maxLength, long maxLatency) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		Collection<MessageId> ids =
				db.getMessagesToSend(txn, c, maxLength, maxLatency);
		long totalLength = 0;
		List<Message> messages = new ArrayList<>(ids.size());
		for (MessageId m : ids) {
			Message message = db.getMessage(txn, m);
			totalLength += message.getRawLength();
			messages.add(message);
			db.updateExpiryTimeAndEta(txn, c, m, maxLatency);
		}
		if (ids.isEmpty()) return null;
		db.lowerRequestedFlag(txn, c, ids);
		transaction.attach(new MessagesSentEvent(c, ids, totalLength));
		return messages;
	}

	@Override
	public Collection<Message> generateBatch(Transaction transaction,
			ContactId c, Collection<MessageId> ids, long maxLatency)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		long totalLength = 0;
		List<Message> messages = new ArrayList<>(ids.size());
		List<MessageId> sentIds = new ArrayList<>(ids.size());
		for (MessageId m : ids) {
			if (db.containsVisibleMessage(txn, c, m)) {
				Message message = db.getMessage(txn, m);
				totalLength += message.getRawLength();
				messages.add(message);
				sentIds.add(m);
				db.updateExpiryTimeAndEta(txn, c, m, maxLatency);
			}
		}
		if (messages.isEmpty()) return messages;
		db.lowerRequestedFlag(txn, c, sentIds);
		transaction.attach(new MessagesSentEvent(c, sentIds, totalLength));
		return messages;
	}

	@Nullable
	@Override
	public Offer generateOffer(Transaction transaction, ContactId c,
			int maxMessages, long maxLatency) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		Collection<MessageId> ids =
				db.getMessagesToOffer(txn, c, maxMessages, maxLatency);
		if (ids.isEmpty()) return null;
		for (MessageId m : ids)
			db.updateExpiryTimeAndEta(txn, c, m, maxLatency);
		return new Offer(ids);
	}

	@Nullable
	@Override
	public Request generateRequest(Transaction transaction, ContactId c,
			int maxMessages) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		Collection<MessageId> ids = db.getMessagesToRequest(txn, c,
				maxMessages);
		if (ids.isEmpty()) return null;
		db.removeOfferedMessages(txn, c, ids);
		return new Request(ids);
	}

	@Nullable
	@Override
	public Collection<Message> generateRequestedBatch(Transaction transaction,
			ContactId c, int maxLength, long maxLatency) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		Collection<MessageId> ids =
				db.getRequestedMessagesToSend(txn, c, maxLength, maxLatency);
		long totalLength = 0;
		List<Message> messages = new ArrayList<>(ids.size());
		for (MessageId m : ids) {
			Message message = db.getMessage(txn, m);
			totalLength += message.getRawLength();
			messages.add(message);
			db.updateExpiryTimeAndEta(txn, c, m, maxLatency);
		}
		if (ids.isEmpty()) return null;
		db.lowerRequestedFlag(txn, c, ids);
		transaction.attach(new MessagesSentEvent(c, ids, totalLength));
		return messages;
	}

	@Override
	public Contact getContact(Transaction transaction, ContactId c)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		return db.getContact(txn, c);
	}

	@Override
	public Collection<Contact> getContacts(Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		return db.getContacts(txn);
	}

	@Override
	public Collection<Contact> getContactsByAuthorId(Transaction transaction,
			AuthorId remote) throws DbException {
		T txn = unbox(transaction);
		return db.getContactsByAuthorId(txn, remote);
	}

	@Override
	public Collection<ContactId> getContacts(Transaction transaction,
			AuthorId local) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsIdentity(txn, local))
			throw new NoSuchIdentityException();
		return db.getContacts(txn, local);
	}

	@Override
	public Group getGroup(Transaction transaction, GroupId g)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		return db.getGroup(txn, g);
	}

	@Override
	public Metadata getGroupMetadata(Transaction transaction, GroupId g)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		return db.getGroupMetadata(txn, g);
	}

	@Override
	public Collection<Group> getGroups(Transaction transaction, ClientId c,
			int majorVersion) throws DbException {
		T txn = unbox(transaction);
		return db.getGroups(txn, c, majorVersion);
	}

	@Override
	public Visibility getGroupVisibility(Transaction transaction, ContactId c,
			GroupId g) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		return db.getGroupVisibility(txn, c, g);
	}

	@Override
	public Identity getIdentity(Transaction transaction, AuthorId a)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsIdentity(txn, a))
			throw new NoSuchIdentityException();
		return db.getIdentity(txn, a);
	}

	@Override
	public Collection<Identity> getIdentities(Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		return db.getIdentities(txn);
	}

	@Override
	public Message getMessage(Transaction transaction, MessageId m)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		return db.getMessage(txn, m);
	}

	@Override
	public Collection<MessageId> getMessageIds(Transaction transaction,
			GroupId g) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		return db.getMessageIds(txn, g);
	}

	@Override
	public Collection<MessageId> getMessageIds(Transaction transaction,
			GroupId g, Metadata query) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		return db.getMessageIds(txn, g, query);
	}

	@Override
	public Collection<MessageId> getMessagesToValidate(Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		return db.getMessagesToValidate(txn);
	}

	@Override
	public Collection<MessageId> getPendingMessages(Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		return db.getPendingMessages(txn);
	}

	@Override
	public Collection<MessageId> getMessagesToShare(Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		return db.getMessagesToShare(txn);
	}

	@Override
	public Map<GroupId, Collection<MessageId>> getMessagesToDelete(
			Transaction transaction) throws DbException {
		T txn = unbox(transaction);
		return db.getMessagesToDelete(txn);
	}

	@Override
	public Map<MessageId, Metadata> getMessageMetadata(Transaction transaction,
			GroupId g) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		return db.getMessageMetadata(txn, g);
	}

	@Override
	public Map<MessageId, Metadata> getMessageMetadata(Transaction transaction,
			GroupId g, Metadata query) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		return db.getMessageMetadata(txn, g, query);
	}

	@Override
	public Metadata getMessageMetadata(Transaction transaction, MessageId m)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		return db.getMessageMetadata(txn, m);
	}

	@Override
	public Metadata getMessageMetadataForValidator(Transaction transaction,
			MessageId m)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		return db.getMessageMetadataForValidator(txn, m);
	}

	@Override
	public MessageState getMessageState(Transaction transaction, MessageId m)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		return db.getMessageState(txn, m);
	}

	@Override
	public Collection<MessageStatus> getMessageStatus(Transaction transaction,
			ContactId c, GroupId g) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		if (db.getGroupVisibility(txn, c, g) == INVISIBLE) {
			// No status rows exist - return default statuses
			Collection<MessageStatus> statuses = new ArrayList<>();
			for (MessageId m : db.getMessageIds(txn, g))
				statuses.add(new MessageStatus(m, c, false, false));
			return statuses;
		}
		return db.getMessageStatus(txn, c, g);
	}

	@Override
	public MessageStatus getMessageStatus(Transaction transaction, ContactId c,
			MessageId m) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		MessageStatus status = db.getMessageStatus(txn, c, m);
		if (status == null) return new MessageStatus(m, c, false, false);
		return status;
	}

	@Override
	public Map<MessageId, Integer> getUnackedMessagesToSend(
			Transaction transaction,
			ContactId c) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		return db.getUnackedMessagesToSend(txn, c);
	}

	@Override
	public long getUnackedMessageBytesToSend(Transaction transaction,
			ContactId c) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		return db.getUnackedMessageBytesToSend(txn, c);
	}

	@Override
	public Map<MessageId, MessageState> getMessageDependencies(
			Transaction transaction, MessageId m) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		return db.getMessageDependencies(txn, m);
	}

	@Override
	public Map<MessageId, MessageState> getMessageDependents(
			Transaction transaction, MessageId m) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		return db.getMessageDependents(txn, m);
	}

	@Override
	public long getNextCleanupDeadline(Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		return db.getNextCleanupDeadline(txn);
	}

	@Override
	public long getNextSendTime(Transaction transaction, ContactId c)
			throws DbException {
		T txn = unbox(transaction);
		return db.getNextSendTime(txn, c);
	}

	@Override
	public PendingContact getPendingContact(Transaction transaction,
			PendingContactId p) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsPendingContact(txn, p))
			throw new NoSuchPendingContactException();
		return db.getPendingContact(txn, p);
	}

	@Override
	public Collection<PendingContact> getPendingContacts(
			Transaction transaction) throws DbException {
		T txn = unbox(transaction);
		return db.getPendingContacts(txn);
	}

	@Override
	public Settings getSettings(Transaction transaction, String namespace)
			throws DbException {
		T txn = unbox(transaction);
		return db.getSettings(txn, namespace);
	}

	@Override
	public List<Byte> getSyncVersions(Transaction transaction, ContactId c)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		return db.getSyncVersions(txn, c);
	}

	@Override
	public Collection<TransportKeySet> getTransportKeys(Transaction transaction,
			TransportId t) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsTransport(txn, t))
			throw new NoSuchTransportException();
		return db.getTransportKeys(txn, t);
	}

	@Override
	public Map<ContactId, Collection<TransportId>> getTransportsWithKeys(
			Transaction transaction) throws DbException {
		T txn = unbox(transaction);
		return db.getTransportsWithKeys(txn);
	}

	@Override
	public void incrementStreamCounter(Transaction transaction, TransportId t,
			KeySetId k) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsTransport(txn, t))
			throw new NoSuchTransportException();
		db.incrementStreamCounter(txn, t, k);
	}

	@Override
	public void mergeGroupMetadata(Transaction transaction, GroupId g,
			Metadata meta) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		db.mergeGroupMetadata(txn, g, meta);
	}

	@Override
	public void mergeMessageMetadata(Transaction transaction, MessageId m,
			Metadata meta) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		db.mergeMessageMetadata(txn, m, meta);
	}

	@Override
	public void mergeSettings(Transaction transaction, Settings s,
			String namespace) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		Settings old = db.getSettings(txn, namespace);
		Settings merged = new Settings();
		merged.putAll(old);
		merged.putAll(s);
		if (!merged.equals(old)) {
			db.mergeSettings(txn, s, namespace);
			transaction.attach(new SettingsUpdatedEvent(namespace, merged));
		}
	}

	@Override
	public void receiveAck(Transaction transaction, ContactId c, Ack a)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		Collection<MessageId> acked = new ArrayList<>();
		for (MessageId m : a.getMessageIds()) {
			if (db.containsVisibleMessage(txn, c, m)) {
				if (db.raiseSeenFlag(txn, c, m)) {
					// This is the first time the message has been acked by
					// this contact. Start the cleanup timer (a no-op unless
					// a cleanup deadline has been set for this message)
					long deadline = db.startCleanupTimer(txn, m);
					if (deadline != TIMER_NOT_STARTED) {
						transaction.attach(new CleanupTimerStartedEvent(m,
								deadline));
					}
					acked.add(m);
				}
			}
		}
		if (acked.size() > 0) {
			transaction.attach(new MessagesAckedEvent(c, acked));
		}
	}

	private String getMessageText(BdfList body) throws FormatException {
		// Message type (0), member (1), parent ID (2), previous message ID (3),
		// text (4), signature (5)
		return body.getString(4);
	}

	private BdfList toList(byte[] b, int off, int len) throws FormatException {
		ByteArrayInputStream in = new ByteArrayInputStream(b, off, len);
		BdfReaderFactoryImpl bdfReaderFactory=  new BdfReaderFactoryImpl();
		BdfReader reader = bdfReaderFactory.createReader(in);
		try {
			BdfList list = reader.readList();
			if (!reader.eof()) throw new FormatException();
			return list;
		} catch (FormatException e) {
			throw e;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public BdfList toList(byte[] b) throws FormatException {
		return toList(b, 0, b.length);
	}

	public BdfList toList(Message m) throws FormatException {
		return toList(m.getBody());
	}

	@Override
	public void receiveMessage(Transaction transaction, ContactId c, Message m)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		if (db.getGroupVisibility(txn, c, m.getGroupId()) != INVISIBLE) {
			if (db.containsMessage(txn, m.getId())) {
				db.raiseSeenFlag(txn, c, m.getId());
				db.raiseAckFlag(txn, c, m.getId());
			} else {
				if((new String(m.getBody()).contains(Message.MARKER_IDENTIFIER))) {
					String text=null;
					HashMap<String,String> keyValues=new HashMap<>();
					try {

						 text = getMessageText(toList(m.getBody()));
						 if(text.trim().startsWith("{") && text.trim().endsWith("}")){
						 	String jsonPart=text.substring(1,text.length()-2);
						 	StringTokenizer jsonTokenizer=new StringTokenizer(jsonPart,",");
						 	while(jsonTokenizer.hasMoreElements()){
						 		String jsonElement=jsonTokenizer.nextToken();
						 		String[] pair=jsonElement.split(":");
						 		if(pair.length==2) {
								    keyValues.put(pair[0].replace("\"",""),pair[1].replace("\"",""));

							    }
						    }
						 }
					}
					catch (Exception e){
						throw new DbException(e);
					}
					if(keyValues.containsKey("action")){
						int action=Integer.parseInt(keyValues.get("action"));
						switch(action){
							case 0:
								db.addMessage(txn, m, UNKNOWN, false, false, c,
										Message.MessageType.MARKER);

								transaction.attach(new MessageAddedEvent(m, c));
								transaction.attach(new MarkerAddedEvent(m,text));
								break;
							case 1:
								String markerID=keyValues.get("id");
								removeMarker(transaction,markerID);
								db.addMessage(txn, m, UNKNOWN, false, false, c,
										Message.MessageType.MARKER);

								transaction.attach(new MessageAddedEvent(m, c));
								transaction.attach(new MarkerAddedEvent(m,text));

								break;
							case 2:
								String removeMarkerID=keyValues.get("id");
								removeMarker(transaction,removeMarkerID);
								break;
						}
					}

				}else
					if(!(new String(m.getBody()).contains(Message.LOCATION_IDENTIFIER))) {
					db.addMessage(txn, m, UNKNOWN, false, false, c,m.getMessageType());
					transaction.attach(new MessageAddedEvent(m, c));

				}else{
					transaction.attach((new LocationMessageEvent(m, c)));

				}

			}
			transaction.attach(new MessageToAckEvent(c));
		}
	}

	private void removeMarker(Transaction transaction,String markerID){

		db.removeMarker(unbox(transaction),markerID);
		transaction.attach(new MarkerRemovedEvent(markerID));
	}

	@Override
	public void receiveOffer(Transaction transaction, ContactId c, Offer o)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		boolean ack = false, request = false;
		int count = db.countOfferedMessages(txn, c);
		for (MessageId m : o.getMessageIds()) {
			if (db.containsVisibleMessage(txn, c, m)) {
				db.raiseSeenFlag(txn, c, m);
				db.raiseAckFlag(txn, c, m);
				ack = true;
			} else if (count < MAX_OFFERED_MESSAGES) {
				db.addOfferedMessage(txn, c, m);
				request = true;
				count++;
			}
		}
		if (ack) transaction.attach(new MessageToAckEvent(c));
		if (request) transaction.attach(new MessageToRequestEvent(c));
	}

	@Override
	public void receiveRequest(Transaction transaction, ContactId c, Request r)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		boolean requested = false;
		for (MessageId m : r.getMessageIds()) {
			if (db.containsVisibleMessage(txn, c, m)) {
				db.raiseRequestedFlag(txn, c, m);
				db.resetExpiryTime(txn, c, m);
				requested = true;
			}
		}
		if (requested) transaction.attach(new MessageRequestedEvent(c));
	}

	@Override
	public void removeContact(Transaction transaction, ContactId c)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		db.removeContact(txn, c);
		transaction.attach(new ContactRemovedEvent(c));
	}

	@Override
	public void removeGroup(Transaction transaction, Group g)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		GroupId id = g.getId();
		if (!db.containsGroup(txn, id))
			throw new NoSuchGroupException();
		Collection<ContactId> affected =
				db.getGroupVisibility(txn, id).keySet();
		db.removeGroup(txn, id);
		transaction.attach(new GroupRemovedEvent(g));
		transaction.attach(new GroupVisibilityUpdatedEvent(affected));
	}

	@Override
	public void removeIdentity(Transaction transaction, AuthorId a)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsIdentity(txn, a))
			throw new NoSuchIdentityException();
		db.removeIdentity(txn, a);
		transaction.attach(new IdentityRemovedEvent(a));
	}

	@Override
	public void removeMessage(Transaction transaction, MessageId m)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		// TODO: Don't allow messages with dependents to be removed
		db.removeMessage(txn, m);
	}

	@Override
	public void removePendingContact(Transaction transaction,
			PendingContactId p) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsPendingContact(txn, p))
			throw new NoSuchPendingContactException();
		db.removePendingContact(txn, p);
		transaction.attach(new PendingContactRemovedEvent(p));
	}

	@Override
	public void removeTemporaryMessages(Transaction transaction)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		db.removeTemporaryMessages(txn);
	}

	@Override
	public void removeTransport(Transaction transaction, TransportId t)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsTransport(txn, t))
			throw new NoSuchTransportException();
		db.removeTransport(txn, t);
	}

	@Override
	public void removeTransportKeys(Transaction transaction, TransportId t,
			KeySetId k) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsTransport(txn, t))
			throw new NoSuchTransportException();
		db.removeTransportKeys(txn, t, k);
	}

	@Override
	public void setCleanupTimerDuration(Transaction transaction, MessageId m,
			long duration) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		db.setCleanupTimerDuration(txn, m, duration);
	}

	@Override
	public void setContactVerified(Transaction transaction, ContactId c)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		db.setContactVerified(txn, c);
		transaction.attach(new ContactVerifiedEvent(c));
	}

	@Override
	public void setContactAlias(Transaction transaction, ContactId c,
			@Nullable String alias) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		transaction.attach(new ContactAliasChangedEvent(c, alias));
		db.setContactAlias(txn, c, alias);
	}

	@Override
	public void setGroupVisibility(Transaction transaction, ContactId c,
			GroupId g, Visibility v) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		Visibility old = db.getGroupVisibility(txn, c, g);
		if (old == v) return;
		if (old == INVISIBLE) db.addGroupVisibility(txn, c, g, v == SHARED);
		else if (v == INVISIBLE) db.removeGroupVisibility(txn, c, g);
		else db.setGroupVisibility(txn, c, g, v == SHARED);
		List<ContactId> affected = Collections.singletonList(c);
		transaction.attach(new GroupVisibilityUpdatedEvent(affected));
	}

	@Override
	public void setMessagePermanent(Transaction transaction, MessageId m)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		db.setMessagePermanent(txn, m);
	}

	@Override
	public void setMessageNotShared(Transaction transaction, MessageId m)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		db.setMessageShared(txn, m, false);
	}

	@Override
	public void setMessageShared(Transaction transaction, MessageId m)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		if (db.getMessageState(txn, m) != DELIVERED)
			throw new IllegalArgumentException("Shared undelivered message");
		db.setMessageShared(txn, m, true);
		transaction.attach(new MessageSharedEvent(m));
	}

	@Override
	public void setMessageState(Transaction transaction, MessageId m,
			MessageState state) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		db.setMessageState(txn, m, state);
		transaction.attach(new MessageStateChangedEvent(m, false, state));
	}

	@Override
	public void addMessageDependencies(Transaction transaction,
			Message dependent, Collection<MessageId> dependencies)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, dependent.getId()))
			throw new NoSuchMessageException();
		MessageState dependentState =
				db.getMessageState(txn, dependent.getId());
		for (MessageId dependency : dependencies) {
			db.addMessageDependency(txn, dependent, dependency, dependentState);
		}
	}

	@Override
	public void setHandshakeKeyPair(Transaction transaction, AuthorId local,
			PublicKey publicKey, PrivateKey privateKey) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsIdentity(txn, local))
			throw new NoSuchIdentityException();
		db.setHandshakeKeyPair(txn, local, publicKey, privateKey);
	}

	@Override
	public void setReorderingWindow(Transaction transaction, KeySetId k,
			TransportId t, long timePeriod, long base, byte[] bitmap)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsTransport(txn, t))
			throw new NoSuchTransportException();
		db.setReorderingWindow(txn, k, t, timePeriod, base, bitmap);
	}

	@Override
	public void setSyncVersions(Transaction transaction, ContactId c,
			List<Byte> supported) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		db.setSyncVersions(txn, c, supported);
		transaction.attach(new SyncVersionsUpdatedEvent(c, supported));
	}

	@Override
	public void setTransportKeysActive(Transaction transaction, TransportId t,
			KeySetId k) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsTransport(txn, t))
			throw new NoSuchTransportException();
		db.setTransportKeysActive(txn, t, k);
	}

	@Override
	public long startCleanupTimer(Transaction transaction, MessageId m)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		long deadline = db.startCleanupTimer(txn, m);
		if (deadline != TIMER_NOT_STARTED) {
			transaction.attach(new CleanupTimerStartedEvent(m, deadline));
		}
		return deadline;
	}

	@Override
	public void stopCleanupTimer(Transaction transaction, MessageId m)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		db.stopCleanupTimer(txn, m);
	}

	@Override
	public void updateTransportKeys(Transaction transaction,
			Collection<TransportKeySet> keys) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		for (TransportKeySet ks : keys) {
			TransportId t = ks.getKeys().getTransportId();
			if (db.containsTransport(txn, t))
				db.updateTransportKeys(txn, ks);
		}
	}

	private class CommitActionVisitor implements Visitor {

		@Override
		public void visit(EventAction a) {
			eventBus.broadcast(a.getEvent());
		}

		@Override
		public void visit(TaskAction a) {
			eventExecutor.execute(a.getTask());
		}
	}
}

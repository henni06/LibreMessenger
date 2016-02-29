package org.briarproject.db;

import org.briarproject.api.DeviceId;
import org.briarproject.api.TransportId;
import org.briarproject.api.contact.Contact;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.db.ContactExistsException;
import org.briarproject.api.db.DatabaseComponent;
import org.briarproject.api.db.DbException;
import org.briarproject.api.db.Metadata;
import org.briarproject.api.db.NoSuchContactException;
import org.briarproject.api.db.NoSuchGroupException;
import org.briarproject.api.db.NoSuchLocalAuthorException;
import org.briarproject.api.db.NoSuchMessageException;
import org.briarproject.api.db.NoSuchTransportException;
import org.briarproject.api.db.Transaction;
import org.briarproject.api.event.ContactAddedEvent;
import org.briarproject.api.event.ContactRemovedEvent;
import org.briarproject.api.event.ContactStatusChangedEvent;
import org.briarproject.api.event.Event;
import org.briarproject.api.event.EventBus;
import org.briarproject.api.event.GroupAddedEvent;
import org.briarproject.api.event.GroupRemovedEvent;
import org.briarproject.api.event.GroupVisibilityUpdatedEvent;
import org.briarproject.api.event.LocalAuthorAddedEvent;
import org.briarproject.api.event.LocalAuthorRemovedEvent;
import org.briarproject.api.event.MessageAddedEvent;
import org.briarproject.api.event.MessageRequestedEvent;
import org.briarproject.api.event.MessageSharedEvent;
import org.briarproject.api.event.MessageToAckEvent;
import org.briarproject.api.event.MessageToRequestEvent;
import org.briarproject.api.event.MessageValidatedEvent;
import org.briarproject.api.event.MessagesAckedEvent;
import org.briarproject.api.event.MessagesSentEvent;
import org.briarproject.api.event.SettingsUpdatedEvent;
import org.briarproject.api.identity.Author;
import org.briarproject.api.identity.AuthorId;
import org.briarproject.api.identity.LocalAuthor;
import org.briarproject.api.lifecycle.ShutdownManager;
import org.briarproject.api.settings.Settings;
import org.briarproject.api.sync.Ack;
import org.briarproject.api.sync.ClientId;
import org.briarproject.api.sync.Group;
import org.briarproject.api.sync.GroupId;
import org.briarproject.api.sync.Message;
import org.briarproject.api.sync.MessageId;
import org.briarproject.api.sync.MessageStatus;
import org.briarproject.api.sync.Offer;
import org.briarproject.api.sync.Request;
import org.briarproject.api.sync.ValidationManager.Validity;
import org.briarproject.api.transport.TransportKeys;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static org.briarproject.api.sync.ValidationManager.Validity.UNKNOWN;
import static org.briarproject.api.sync.ValidationManager.Validity.VALID;
import static org.briarproject.db.DatabaseConstants.MAX_OFFERED_MESSAGES;

class DatabaseComponentImpl<T> implements DatabaseComponent {

	private static final Logger LOG =
			Logger.getLogger(DatabaseComponentImpl.class.getName());

	private final Database<T> db;
	private final Class<T> txnClass;
	private final EventBus eventBus;
	private final ShutdownManager shutdown;
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

	private volatile int shutdownHandle = -1;

	@Inject
	DatabaseComponentImpl(Database<T> db, Class<T> txnClass, EventBus eventBus,
			ShutdownManager shutdown) {
		this.db = db;
		this.txnClass = txnClass;
		this.eventBus = eventBus;
		this.shutdown = shutdown;
	}

	public boolean open() throws DbException {
		Runnable shutdownHook = new Runnable() {
			public void run() {
				try {
					close();
				} catch (DbException e) {
					if (LOG.isLoggable(WARNING))
						LOG.log(WARNING, e.toString(), e);
				} catch (IOException e) {
					if (LOG.isLoggable(WARNING))
						LOG.log(WARNING, e.toString(), e);
				}
			}
		};
		boolean reopened = db.open();
		shutdownHandle = shutdown.addShutdownHook(shutdownHook);
		return reopened;
	}

	public void close() throws DbException, IOException {
		if (closed.getAndSet(true)) return;
		shutdown.removeShutdownHook(shutdownHandle);
		db.close();
	}

	public Transaction startTransaction(boolean readOnly) throws DbException {
		if (readOnly) lock.readLock().lock();
		else lock.writeLock().lock();
		try {
			return new Transaction(db.startTransaction(), readOnly);
		} catch (DbException e) {
			if (readOnly) lock.readLock().unlock();
			else lock.writeLock().unlock();
			throw e;
		} catch (RuntimeException e) {
			if (readOnly) lock.readLock().unlock();
			else lock.writeLock().unlock();
			throw e;
		}
	}

	public void endTransaction(Transaction transaction) throws DbException {
		try {
			T txn = txnClass.cast(transaction.unbox());
			if (transaction.isComplete()) db.commitTransaction(txn);
			else db.abortTransaction(txn);
		} finally {
			if (transaction.isReadOnly()) lock.readLock().unlock();
			else lock.writeLock().unlock();
		}
		if (transaction.isComplete())
			for (Event e : transaction.getEvents()) eventBus.broadcast(e);
	}

	private T unbox(Transaction transaction) {
		if (transaction.isComplete()) throw new IllegalStateException();
		return txnClass.cast(transaction.unbox());
	}

	public ContactId addContact(Transaction transaction, Author remote,
			AuthorId local, boolean active) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsLocalAuthor(txn, local))
			throw new NoSuchLocalAuthorException();
		if (db.containsContact(txn, remote.getId(), local))
			throw new ContactExistsException();
		ContactId c = db.addContact(txn, remote, local, active);
		transaction.attach(new ContactAddedEvent(c, active));
		if (active) transaction.attach(new ContactStatusChangedEvent(c, true));
		return c;
	}

	public void addGroup(Transaction transaction, Group g) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, g.getId())) {
			db.addGroup(txn, g);
			transaction.attach(new GroupAddedEvent(g));
		}
	}

	public void addLocalAuthor(Transaction transaction, LocalAuthor a)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsLocalAuthor(txn, a.getId())) {
			db.addLocalAuthor(txn, a);
			transaction.attach(new LocalAuthorAddedEvent(a.getId()));
		}
	}

	public void addLocalMessage(Transaction transaction, Message m, ClientId c,
			Metadata meta, boolean shared) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, m.getGroupId()))
			throw new NoSuchGroupException();
		if (!db.containsMessage(txn, m.getId())) {
			addMessage(txn, m, VALID, shared);
			transaction.attach(new MessageAddedEvent(m, null));
			transaction.attach(new MessageValidatedEvent(m, c, true, true));
			if (shared) transaction.attach(new MessageSharedEvent(m));
		}
		db.mergeMessageMetadata(txn, m.getId(), meta);
	}

	private void addMessage(T txn, Message m, Validity validity, boolean shared)
			throws DbException {
		db.addMessage(txn, m, validity, shared);
		for (ContactId c : db.getVisibility(txn, m.getGroupId())) {
			boolean offered = db.removeOfferedMessage(txn, c, m.getId());
			db.addStatus(txn, c, m.getId(), offered, offered);
		}
	}

	public void addTransport(Transaction transaction, TransportId t,
			int maxLatency) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsTransport(txn, t))
			db.addTransport(txn, t, maxLatency);
	}

	public void addTransportKeys(Transaction transaction, ContactId c,
			TransportKeys k) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		if (!db.containsTransport(txn, k.getTransportId()))
			throw new NoSuchTransportException();
		db.addTransportKeys(txn, c, k);
	}

	public void deleteMessage(Transaction transaction, MessageId m)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		db.deleteMessage(txn, m);
	}

	public void deleteMessageMetadata(Transaction transaction, MessageId m)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		db.deleteMessageMetadata(txn, m);
	}

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

	public Collection<byte[]> generateBatch(Transaction transaction,
			ContactId c, int maxLength, int maxLatency) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		Collection<MessageId> ids = db.getMessagesToSend(txn, c, maxLength);
		List<byte[]> messages = new ArrayList<byte[]>(ids.size());
		for (MessageId m : ids) {
			messages.add(db.getRawMessage(txn, m));
			db.updateExpiryTime(txn, c, m, maxLatency);
		}
		if (ids.isEmpty()) return null;
		db.lowerRequestedFlag(txn, c, ids);
		transaction.attach(new MessagesSentEvent(c, ids));
		return Collections.unmodifiableList(messages);
	}

	public Offer generateOffer(Transaction transaction, ContactId c,
			int maxMessages, int maxLatency) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		Collection<MessageId> ids = db.getMessagesToOffer(txn, c, maxMessages);
		if (ids.isEmpty()) return null;
		for (MessageId m : ids) db.updateExpiryTime(txn, c, m, maxLatency);
		return new Offer(ids);
	}

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

	public Collection<byte[]> generateRequestedBatch(Transaction transaction,
			ContactId c, int maxLength, int maxLatency) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		Collection<MessageId> ids = db.getRequestedMessagesToSend(txn, c,
				maxLength);
		List<byte[]> messages = new ArrayList<byte[]>(ids.size());
		for (MessageId m : ids) {
			messages.add(db.getRawMessage(txn, m));
			db.updateExpiryTime(txn, c, m, maxLatency);
		}
		if (ids.isEmpty()) return null;
		db.lowerRequestedFlag(txn, c, ids);
		transaction.attach(new MessagesSentEvent(c, ids));
		return Collections.unmodifiableList(messages);
	}

	public Contact getContact(Transaction transaction, ContactId c)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		return db.getContact(txn, c);
	}

	public Collection<Contact> getContacts(Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		return db.getContacts(txn);
	}

	public Collection<ContactId> getContacts(Transaction transaction,
			AuthorId a) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsLocalAuthor(txn, a))
			throw new NoSuchLocalAuthorException();
		return db.getContacts(txn, a);
	}

	public boolean containsContact(Transaction transaction, AuthorId remote,
			AuthorId local) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsLocalAuthor(txn, local))
			throw new NoSuchLocalAuthorException();
		return db.containsContact(txn, remote, local);
	}

	public DeviceId getDeviceId(Transaction transaction) throws DbException {
		T txn = unbox(transaction);
		return db.getDeviceId(txn);
	}

	public Group getGroup(Transaction transaction, GroupId g)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		return db.getGroup(txn, g);
	}

	public Metadata getGroupMetadata(Transaction transaction, GroupId g)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		return db.getGroupMetadata(txn, g);
	}

	public Collection<Group> getGroups(Transaction transaction, ClientId c)
			throws DbException {
		T txn = unbox(transaction);
		return db.getGroups(txn, c);
	}

	public LocalAuthor getLocalAuthor(Transaction transaction, AuthorId a)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsLocalAuthor(txn, a))
			throw new NoSuchLocalAuthorException();
		return db.getLocalAuthor(txn, a);
	}

	public Collection<LocalAuthor> getLocalAuthors(Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		return db.getLocalAuthors(txn);
	}

	public Collection<MessageId> getMessagesToValidate(Transaction transaction,
			ClientId c) throws DbException {
		T txn = unbox(transaction);
		return db.getMessagesToValidate(txn, c);
	}

	public byte[] getRawMessage(Transaction transaction, MessageId m)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		return db.getRawMessage(txn, m);
	}

	public Map<MessageId, Metadata> getMessageMetadata(Transaction transaction,
			GroupId g) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		return db.getMessageMetadata(txn, g);
	}

	public Metadata getMessageMetadata(Transaction transaction, MessageId m)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		return db.getMessageMetadata(txn, m);
	}

	public Collection<MessageStatus> getMessageStatus(Transaction transaction,
			ContactId c, GroupId g) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		return db.getMessageStatus(txn, c, g);
	}

	public MessageStatus getMessageStatus(Transaction transaction, ContactId c,
			MessageId m) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		return db.getMessageStatus(txn, c, m);
	}

	public Settings getSettings(Transaction transaction, String namespace)
			throws DbException {
		T txn = unbox(transaction);
		return db.getSettings(txn, namespace);
	}

	public Map<ContactId, TransportKeys> getTransportKeys(
			Transaction transaction, TransportId t) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsTransport(txn, t))
			throw new NoSuchTransportException();
		return db.getTransportKeys(txn, t);
	}

	public void incrementStreamCounter(Transaction transaction, ContactId c,
			TransportId t, long rotationPeriod) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		if (!db.containsTransport(txn, t))
			throw new NoSuchTransportException();
		db.incrementStreamCounter(txn, c, t, rotationPeriod);
	}

	public boolean isVisibleToContact(Transaction transaction, ContactId c,
			GroupId g) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		return db.containsVisibleGroup(txn, c, g);
	}

	public void mergeGroupMetadata(Transaction transaction, GroupId g,
			Metadata meta) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		db.mergeGroupMetadata(txn, g, meta);
	}

	public void mergeMessageMetadata(Transaction transaction, MessageId m,
			Metadata meta) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m))
			throw new NoSuchMessageException();
		db.mergeMessageMetadata(txn, m, meta);
	}

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
			transaction.attach(new SettingsUpdatedEvent(namespace));
		}
	}

	public void receiveAck(Transaction transaction, ContactId c, Ack a)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		Collection<MessageId> acked = new ArrayList<MessageId>();
		for (MessageId m : a.getMessageIds()) {
			if (db.containsVisibleMessage(txn, c, m)) {
				db.raiseSeenFlag(txn, c, m);
				acked.add(m);
			}
		}
		transaction.attach(new MessagesAckedEvent(c, acked));
	}

	public void receiveMessage(Transaction transaction, ContactId c, Message m)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		if (db.containsVisibleGroup(txn, c, m.getGroupId())) {
			if (!db.containsMessage(txn, m.getId())) {
				addMessage(txn, m, UNKNOWN, false);
				transaction.attach(new MessageAddedEvent(m, c));
			}
			db.raiseAckFlag(txn, c, m.getId());
			transaction.attach(new MessageToAckEvent(c));
		}
	}

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

	public void removeContact(Transaction transaction, ContactId c)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		db.removeContact(txn, c);
		transaction.attach(new ContactRemovedEvent(c));
	}

	public void removeGroup(Transaction transaction, Group g)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		GroupId id = g.getId();
		if (!db.containsGroup(txn, id))
			throw new NoSuchGroupException();
		Collection<ContactId> affected = db.getVisibility(txn, id);
		db.removeGroup(txn, id);
		transaction.attach(new GroupRemovedEvent(g));
		transaction.attach(new GroupVisibilityUpdatedEvent(affected));
	}

	public void removeLocalAuthor(Transaction transaction, AuthorId a)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsLocalAuthor(txn, a))
			throw new NoSuchLocalAuthorException();
		db.removeLocalAuthor(txn, a);
		transaction.attach(new LocalAuthorRemovedEvent(a));
	}

	public void removeTransport(Transaction transaction, TransportId t)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsTransport(txn, t))
			throw new NoSuchTransportException();
		db.removeTransport(txn, t);
	}

	public void setContactActive(Transaction transaction, ContactId c,
			boolean active) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		db.setContactActive(txn, c, active);
		transaction.attach(new ContactStatusChangedEvent(c, active));
	}

	public void setMessageShared(Transaction transaction, Message m,
			boolean shared) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m.getId()))
			throw new NoSuchMessageException();
		db.setMessageShared(txn, m.getId(), shared);
		if (shared) transaction.attach(new MessageSharedEvent(m));
	}

	public void setMessageValid(Transaction transaction, Message m, ClientId c,
			boolean valid) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, m.getId()))
			throw new NoSuchMessageException();
		db.setMessageValid(txn, m.getId(), valid);
		transaction.attach(new MessageValidatedEvent(m, c, false, valid));
	}

	public void setReorderingWindow(Transaction transaction, ContactId c,
			TransportId t, long rotationPeriod, long base, byte[] bitmap)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		if (!db.containsTransport(txn, t))
			throw new NoSuchTransportException();
		db.setReorderingWindow(txn, c, t, rotationPeriod, base, bitmap);
	}

	public void setVisibleToContact(Transaction transaction, ContactId c,
			GroupId g, boolean visible) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		if (!db.containsGroup(txn, g))
			throw new NoSuchGroupException();
		boolean wasVisible = db.containsVisibleGroup(txn, c, g);
		if (visible && !wasVisible) {
			db.addVisibility(txn, c, g);
			for (MessageId m : db.getMessageIds(txn, g)) {
				boolean seen = db.removeOfferedMessage(txn, c, m);
				db.addStatus(txn, c, m, seen, seen);
			}
		} else if (!visible && wasVisible) {
			db.removeVisibility(txn, c, g);
			for (MessageId m : db.getMessageIds(txn, g))
				db.removeStatus(txn, c, m);
		}
		if (visible != wasVisible) {
			List<ContactId> affected = Collections.singletonList(c);
			transaction.attach(new GroupVisibilityUpdatedEvent(affected));
		}
	}

	public void updateTransportKeys(Transaction transaction,
			Map<ContactId, TransportKeys> keys) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		Map<ContactId, TransportKeys> filtered =
				new HashMap<ContactId, TransportKeys>();
		for (Entry<ContactId, TransportKeys> e : keys.entrySet()) {
			ContactId c = e.getKey();
			TransportKeys k = e.getValue();
			if (db.containsContact(txn, c)
					&& db.containsTransport(txn, k.getTransportId())) {
				filtered.put(c, k);
			}
		}
		db.updateTransportKeys(txn, filtered);
	}
}

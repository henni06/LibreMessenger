package org.briarproject.bramble.transport;

import org.briarproject.bramble.api.Bytes;
import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.contact.PendingContactId;
import org.briarproject.bramble.api.crypto.SecretKey;
import org.briarproject.bramble.api.crypto.TransportCrypto;
import org.briarproject.bramble.api.db.DatabaseComponent;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.Transaction;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.system.Clock;
import org.briarproject.bramble.api.system.Scheduler;
import org.briarproject.bramble.api.transport.KeySetId;
import org.briarproject.bramble.api.transport.StreamContext;
import org.briarproject.bramble.api.transport.TransportKeySet;
import org.briarproject.bramble.api.transport.TransportKeys;
import org.briarproject.bramble.transport.ReorderingWindow.Change;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.briarproject.bramble.api.transport.TransportConstants.MAX_CLOCK_DIFFERENCE;
import static org.briarproject.bramble.api.transport.TransportConstants.PROTOCOL_VERSION;
import static org.briarproject.bramble.api.transport.TransportConstants.TAG_LENGTH;
import static org.briarproject.bramble.util.ByteUtils.MAX_32_BIT_UNSIGNED;
import static org.briarproject.bramble.util.LogUtils.logException;

@ThreadSafe
@NotNullByDefault
class TransportKeyManagerImpl implements TransportKeyManager {

	private static final Logger LOG =
			getLogger(TransportKeyManagerImpl.class.getName());

	private final DatabaseComponent db;
	private final TransportCrypto transportCrypto;
	private final Executor dbExecutor;
	private final ScheduledExecutorService scheduler;
	private final Clock clock;
	private final TransportId transportId;
	private final long timePeriodLength;
	private final AtomicBoolean used = new AtomicBoolean(false);
	private final ReentrantLock lock = new ReentrantLock();

	@GuardedBy("lock")
	private final Map<KeySetId, MutableTransportKeySet> keys = new HashMap<>();
	@GuardedBy("lock")
	private final Map<Bytes, TagContext> inContexts = new HashMap<>();
	@GuardedBy("lock")
	private final Map<ContactId, MutableTransportKeySet> outContexts =
			new HashMap<>();

	TransportKeyManagerImpl(DatabaseComponent db,
			TransportCrypto transportCrypto, Executor dbExecutor,
			@Scheduler ScheduledExecutorService scheduler, Clock clock,
			TransportId transportId, long maxLatency) {
		this.db = db;
		this.transportCrypto = transportCrypto;
		this.dbExecutor = dbExecutor;
		this.scheduler = scheduler;
		this.clock = clock;
		this.transportId = transportId;
		timePeriodLength = maxLatency + MAX_CLOCK_DIFFERENCE;
	}

	@Override
	public void start(Transaction txn) throws DbException {
		if (used.getAndSet(true)) throw new IllegalStateException();
		long now = clock.currentTimeMillis();
		lock.lock();
		try {
			// Load the transport keys from the DB
			Collection<TransportKeySet> loaded =
					db.getTransportKeys(txn, transportId);
			// Update the keys to the current time period
			UpdateResult updateResult = updateKeys(loaded, now);
			// Initialise mutable state for all contacts
			addKeys(updateResult.current);
			// Write any updated keys back to the DB
			if (!updateResult.updated.isEmpty())
				db.updateTransportKeys(txn, updateResult.updated);
		} finally {
			lock.unlock();
		}
		// Schedule the next key update
		scheduleKeyUpdate(now);
	}

	private UpdateResult updateKeys(Collection<TransportKeySet> keys,
			long now) {
		UpdateResult updateResult = new UpdateResult();
		long timePeriod = now / timePeriodLength;
		for (TransportKeySet ks : keys) {
			TransportKeys k = ks.getKeys();
			TransportKeys k1 = transportCrypto.updateTransportKeys(k,
					timePeriod);
			TransportKeySet ks1 = new TransportKeySet(ks.getKeySetId(),
					ks.getContactId(), null, k1);
			if (k1.getTimePeriod() > k.getTimePeriod())
				updateResult.updated.add(ks1);
			updateResult.current.add(ks1);
		}
		return updateResult;
	}

	@GuardedBy("lock")
	private void addKeys(Collection<TransportKeySet> keys) {
		for (TransportKeySet ks : keys) {
			addKeys(ks.getKeySetId(), ks.getContactId(),
					ks.getPendingContactId(),
					new MutableTransportKeys(ks.getKeys()));
		}
	}

	@GuardedBy("lock")
	private void addKeys(KeySetId keySetId, @Nullable ContactId contactId,
			@Nullable PendingContactId pendingContactId,
			MutableTransportKeys keys) {
		MutableTransportKeySet ks = new MutableTransportKeySet(keySetId,
				contactId, pendingContactId, keys);
		this.keys.put(keySetId, ks);
		boolean handshakeMode = keys.isHandshakeMode();
		encodeTags(keySetId, contactId, pendingContactId,
				keys.getPreviousIncomingKeys(), handshakeMode);
		encodeTags(keySetId, contactId, pendingContactId,
				keys.getCurrentIncomingKeys(), handshakeMode);
		encodeTags(keySetId, contactId, pendingContactId,
				keys.getNextIncomingKeys(), handshakeMode);
		considerReplacingOutgoingKeys(ks);
	}

	@GuardedBy("lock")
	private void encodeTags(KeySetId keySetId, @Nullable ContactId contactId,
			@Nullable PendingContactId pendingContactId,
			MutableIncomingKeys inKeys, boolean handshakeMode) {
		for (long streamNumber : inKeys.getWindow().getUnseen()) {
			TagContext tagCtx = new TagContext(keySetId, contactId,
					pendingContactId, inKeys, streamNumber, handshakeMode);
			byte[] tag = new byte[TAG_LENGTH];
			transportCrypto.encodeTag(tag, inKeys.getTagKey(), PROTOCOL_VERSION,
					streamNumber);
			inContexts.put(new Bytes(tag), tagCtx);
		}
	}

	@GuardedBy("lock")
	private void considerReplacingOutgoingKeys(MutableTransportKeySet ks) {
		// Use the active outgoing keys with the highest key set ID
		ContactId c = ks.getContactId();
		if (c != null && ks.getKeys().getCurrentOutgoingKeys().isActive()) {
			MutableTransportKeySet old = outContexts.get(c);
			if (old == null ||
					(old.getKeys().isHandshakeMode() &&
							!ks.getKeys().isHandshakeMode()) ||
					old.getKeySetId().getInt() < ks.getKeySetId().getInt()) {
				outContexts.put(c, ks);
			}
		}
	}

	private void scheduleKeyUpdate(long now) {
		long delay = timePeriodLength - now % timePeriodLength;
		scheduler.schedule((Runnable) this::updateKeys, delay, MILLISECONDS);
	}

	private void updateKeys() {
		dbExecutor.execute(() -> {
			try {
				db.transaction(false, this::updateKeys);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	@Override
	public KeySetId addContact(Transaction txn, ContactId c, SecretKey rootKey,
			long timestamp, boolean alice, boolean active) throws DbException {
		lock.lock();
		try {
			// Work out what time period the timestamp belongs to
			long timePeriod = timestamp / timePeriodLength;
			// Derive the transport keys
			TransportKeys k = transportCrypto.deriveRotationKeys(transportId,
					rootKey, timePeriod, alice, active);
			// Update the keys to the current time period if necessary
			timePeriod = clock.currentTimeMillis() / timePeriodLength;
			k = transportCrypto.updateTransportKeys(k, timePeriod);
			// Write the keys back to the DB
			KeySetId keySetId = db.addTransportKeys(txn, c, k);
			// Initialise mutable state for the contact
			addKeys(keySetId, c, null, new MutableTransportKeys(k));
			return keySetId;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public KeySetId addContact(Transaction txn, ContactId c, SecretKey rootKey,
			boolean alice) throws DbException {
		lock.lock();
		try {
			// Work out what time period we're in
			long timePeriod = clock.currentTimeMillis() / timePeriodLength;
			// Derive the transport keys
			TransportKeys k = transportCrypto.deriveHandshakeKeys(transportId,
					rootKey, timePeriod, alice);
			// Write the keys back to the DB
			KeySetId keySetId = db.addTransportKeys(txn, c, k);
			// Initialise mutable state for the contact
			addKeys(keySetId, c, null, new MutableTransportKeys(k));
			return keySetId;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void activateKeys(Transaction txn, KeySetId k) throws DbException {
		lock.lock();
		try {
			MutableTransportKeySet ks = keys.get(k);
			if (ks == null) throw new IllegalArgumentException();
			MutableTransportKeys m = ks.getKeys();
			m.getCurrentOutgoingKeys().activate();
			considerReplacingOutgoingKeys(ks);
			db.setTransportKeysActive(txn, m.getTransportId(), k);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void removeContact(ContactId c) {
		lock.lock();
		try {
			// Remove mutable state for the contact
			Iterator<TagContext> it = inContexts.values().iterator();
			while (it.hasNext()) if (c.equals(it.next().contactId)) it.remove();
			outContexts.remove(c);
			Iterator<MutableTransportKeySet> it1 = keys.values().iterator();
			while (it1.hasNext())
				if (c.equals(it1.next().getContactId())) it1.remove();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean canSendOutgoingStreams(ContactId c) {
		lock.lock();
		try {
			MutableTransportKeySet ks = outContexts.get(c);
			if (ks == null) return false;
			MutableOutgoingKeys outKeys =
					ks.getKeys().getCurrentOutgoingKeys();
			if (!outKeys.isActive()) throw new AssertionError();
			return outKeys.getStreamCounter() <= MAX_32_BIT_UNSIGNED;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public StreamContext getStreamContext(Transaction txn, ContactId c)
			throws DbException {
		lock.lock();
		try {
			// Look up the outgoing keys for the contact
			MutableTransportKeySet ks = outContexts.get(c);
			if (ks == null) return null;
			MutableTransportKeys keys = ks.getKeys();
			MutableOutgoingKeys outKeys = keys.getCurrentOutgoingKeys();
			if (!outKeys.isActive()) throw new AssertionError();
			if (outKeys.getStreamCounter() > MAX_32_BIT_UNSIGNED) return null;
			// Create a stream context
			StreamContext ctx = new StreamContext(c, null, transportId,
					outKeys.getTagKey(), outKeys.getHeaderKey(),
					outKeys.getStreamCounter(), keys.isHandshakeMode());
			// Increment the stream counter and write it back to the DB
			outKeys.incrementStreamCounter();
			db.incrementStreamCounter(txn, transportId, ks.getKeySetId());
			return ctx;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public StreamContext getStreamContext(Transaction txn, byte[] tag)
			throws DbException {
		lock.lock();
		try {
			// Look up the incoming keys for the tag
			TagContext tagCtx = inContexts.remove(new Bytes(tag));
			if (tagCtx == null) return null;
			MutableIncomingKeys inKeys = tagCtx.inKeys;
			// Create a stream context
			StreamContext ctx = new StreamContext(tagCtx.contactId,
					tagCtx.pendingContactId, transportId,
					inKeys.getTagKey(), inKeys.getHeaderKey(),
					tagCtx.streamNumber, tagCtx.handshakeMode);
			// Update the reordering window
			ReorderingWindow window = inKeys.getWindow();
			Change change = window.setSeen(tagCtx.streamNumber);
			// Add tags for any stream numbers added to the window
			for (long streamNumber : change.getAdded()) {
				byte[] addTag = new byte[TAG_LENGTH];
				transportCrypto.encodeTag(addTag, inKeys.getTagKey(),
						PROTOCOL_VERSION, streamNumber);
				TagContext tagCtx1 = new TagContext(tagCtx.keySetId,
						tagCtx.contactId, tagCtx.pendingContactId, inKeys,
						streamNumber, tagCtx.handshakeMode);
				inContexts.put(new Bytes(addTag), tagCtx1);
			}
			// Remove tags for any stream numbers removed from the window
			for (long streamNumber : change.getRemoved()) {
				if (streamNumber == tagCtx.streamNumber) continue;
				byte[] removeTag = new byte[TAG_LENGTH];
				transportCrypto.encodeTag(removeTag, inKeys.getTagKey(),
						PROTOCOL_VERSION, streamNumber);
				inContexts.remove(new Bytes(removeTag));
			}
			// Write the window back to the DB
			db.setReorderingWindow(txn, tagCtx.keySetId, transportId,
					inKeys.getTimePeriod(), window.getBase(),
					window.getBitmap());
			// If the outgoing keys are inactive, activate them
			MutableTransportKeySet ks = keys.get(tagCtx.keySetId);
			MutableOutgoingKeys outKeys =
					ks.getKeys().getCurrentOutgoingKeys();
			if (!outKeys.isActive()) {
				LOG.info("Activating outgoing keys");
				outKeys.activate();
				considerReplacingOutgoingKeys(ks);
				db.setTransportKeysActive(txn, transportId, tagCtx.keySetId);
			}
			return ctx;
		} finally {
			lock.unlock();
		}
	}

	private void updateKeys(Transaction txn) throws DbException {
		long now = clock.currentTimeMillis();
		lock.lock();
		try {
			// Update the keys to the current time period
			Collection<TransportKeySet> snapshot = new ArrayList<>(keys.size());
			for (MutableTransportKeySet ks : keys.values()) {
				snapshot.add(new TransportKeySet(ks.getKeySetId(),
						ks.getContactId(), ks.getPendingContactId(),
						ks.getKeys().snapshot()));
			}
			UpdateResult updateResult = updateKeys(snapshot, now);
			// Rebuild the mutable state for all contacts
			inContexts.clear();
			outContexts.clear();
			keys.clear();
			addKeys(updateResult.current);
			// Write any updated keys back to the DB
			if (!updateResult.updated.isEmpty())
				db.updateTransportKeys(txn, updateResult.updated);
		} finally {
			lock.unlock();
		}
		// Schedule the next key update
		scheduleKeyUpdate(now);
	}

	private static class TagContext {

		private final KeySetId keySetId;
		@Nullable
		private final ContactId contactId;
		@Nullable
		private final PendingContactId pendingContactId;
		private final MutableIncomingKeys inKeys;
		private final long streamNumber;
		private final boolean handshakeMode;

		private TagContext(KeySetId keySetId, @Nullable ContactId contactId,
				@Nullable PendingContactId pendingContactId,
				MutableIncomingKeys inKeys, long streamNumber,
				boolean handshakeMode) {
			this.keySetId = keySetId;
			this.contactId = contactId;
			this.pendingContactId = pendingContactId;
			this.inKeys = inKeys;
			this.streamNumber = streamNumber;
			this.handshakeMode = handshakeMode;
		}
	}

	private static class UpdateResult {

		private final Collection<TransportKeySet> current = new ArrayList<>();
		private final Collection<TransportKeySet> updated = new ArrayList<>();
	}
}

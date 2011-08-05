package net.sf.briar.db;

import java.io.File;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;
import net.sf.briar.TestUtils;
import net.sf.briar.api.ContactId;
import net.sf.briar.api.Rating;
import net.sf.briar.api.crypto.Password;
import net.sf.briar.api.db.DbException;
import net.sf.briar.api.db.Status;
import net.sf.briar.api.protocol.AuthorId;
import net.sf.briar.api.protocol.BatchId;
import net.sf.briar.api.protocol.Group;
import net.sf.briar.api.protocol.GroupFactory;
import net.sf.briar.api.protocol.GroupId;
import net.sf.briar.api.protocol.Message;
import net.sf.briar.api.protocol.MessageId;
import net.sf.briar.crypto.CryptoModule;
import net.sf.briar.protocol.ProtocolModule;
import net.sf.briar.serial.SerialModule;

import org.apache.commons.io.FileSystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class H2DatabaseTest extends TestCase {

	private static final int ONE_MEGABYTE = 1024 * 1024;
	private static final int MAX_SIZE = 5 * ONE_MEGABYTE;

	private final File testDir = TestUtils.getTestDirectory();
	// The password has the format <file password> <space> <user password>
	private final String passwordString = "foo bar";
	private final Password password = new TestPassword();
	private final Random random = new Random();
	private final GroupFactory groupFactory;

	private final AuthorId authorId;
	private final BatchId batchId;
	private final ContactId contactId;
	private final GroupId groupId;
	private final MessageId messageId;
	private final long timestamp;
	private final int size;
	private final byte[] raw;
	private final Message message;
	private final Group group;
	private final Map<String, Map<String, String>> transports;
	private final Map<Group, Long> subscriptions;

	public H2DatabaseTest() throws Exception {
		super();
		Injector i = Guice.createInjector(new CryptoModule(),
				new ProtocolModule(), new SerialModule());
		groupFactory = i.getInstance(GroupFactory.class);
		authorId = new AuthorId(TestUtils.getRandomId());
		batchId = new BatchId(TestUtils.getRandomId());
		contactId = new ContactId(1);
		groupId = new GroupId(TestUtils.getRandomId());
		messageId = new MessageId(TestUtils.getRandomId());
		timestamp = System.currentTimeMillis();
		size = 1234;
		raw = new byte[size];
		random.nextBytes(raw);
		message = new TestMessage(messageId, MessageId.NONE, groupId, authorId,
				timestamp, raw);
		group = groupFactory.createGroup(groupId, "Group name", null);
		transports = Collections.singletonMap("foo",
				Collections.singletonMap("bar", "baz"));
		subscriptions = Collections.singletonMap(group, 0L);
	}

	@Before
	public void setUp() {
		testDir.mkdirs();
	}

	@Test
	public void testPersistence() throws DbException {
		// Store some records
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();
		assertFalse(db.containsContact(txn, contactId));
		assertEquals(contactId, db.addContact(txn, transports));
		assertTrue(db.containsContact(txn, contactId));
		assertFalse(db.containsSubscription(txn, groupId));
		db.addSubscription(txn, group);
		assertTrue(db.containsSubscription(txn, groupId));
		assertFalse(db.containsMessage(txn, messageId));
		db.addMessage(txn, message);
		assertTrue(db.containsMessage(txn, messageId));
		db.commitTransaction(txn);
		db.close();

		// Check that the records are still there
		db = open(true);
		txn = db.startTransaction();
		assertTrue(db.containsContact(txn, contactId));
		assertEquals(transports, db.getTransports(txn, contactId));
		assertTrue(db.containsSubscription(txn, groupId));
		assertTrue(db.containsMessage(txn, messageId));
		byte[] raw1 = db.getMessage(txn, messageId);
		assertTrue(Arrays.equals(raw, raw1));
		// Delete the records
		db.removeContact(txn, contactId);
		db.removeMessage(txn, messageId);
		db.removeSubscription(txn, groupId);
		db.commitTransaction(txn);
		db.close();

		// Check that the records are gone
		db = open(true);
		txn = db.startTransaction();
		assertFalse(db.containsContact(txn, contactId));
		assertEquals(Collections.emptyMap(), db.getTransports(txn, contactId));
		assertFalse(db.containsSubscription(txn, groupId));
		assertFalse(db.containsMessage(txn, messageId));
		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testContactIdsIncrease() throws DbException {
		ContactId contactId1 = new ContactId(2);
		ContactId contactId2 = new ContactId(3);
		ContactId contactId3 = new ContactId(4);
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Create three contacts
		assertFalse(db.containsContact(txn, contactId));
		assertEquals(contactId, db.addContact(txn, transports));
		assertTrue(db.containsContact(txn, contactId));
		assertFalse(db.containsContact(txn, contactId1));
		assertEquals(contactId1, db.addContact(txn, transports));
		assertTrue(db.containsContact(txn, contactId1));
		assertFalse(db.containsContact(txn, contactId2));
		assertEquals(contactId2, db.addContact(txn, transports));
		assertTrue(db.containsContact(txn, contactId2));
		// Delete one of the contacts
		db.removeContact(txn, contactId1);
		assertFalse(db.containsContact(txn, contactId1));
		// Add another contact - a new ID should be created
		assertFalse(db.containsContact(txn, contactId3));
		assertEquals(contactId3, db.addContact(txn, transports));
		assertTrue(db.containsContact(txn, contactId3));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testRatings() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Unknown authors should be unrated
		assertEquals(Rating.UNRATED, db.getRating(txn, authorId));
		// Store a rating
		db.setRating(txn, authorId, Rating.GOOD);
		// Check that the rating was stored
		assertEquals(Rating.GOOD, db.getRating(txn, authorId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testUnsubscribingRemovesMessage() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Subscribe to a group and store a message
		db.addSubscription(txn, group);
		db.addMessage(txn, message);

		// Unsubscribing from the group should delete the message
		assertTrue(db.containsMessage(txn, messageId));
		db.removeSubscription(txn, groupId);
		assertFalse(db.containsMessage(txn, messageId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testSendableMessagesMustBeSendable() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		db.addMessage(txn, message);
		db.setStatus(txn, contactId, messageId, Status.NEW);

		// The message should not be sendable
		assertEquals(0, db.getSendability(txn, messageId));
		assertFalse(db.hasSendableMessages(txn, contactId));
		Iterator<MessageId> it =
			db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertFalse(it.hasNext());

		// Changing the sendability to > 0 should make the message sendable
		db.setSendability(txn, messageId, 1);
		assertTrue(db.hasSendableMessages(txn, contactId));
		it = db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertTrue(it.hasNext());
		assertEquals(messageId, it.next());
		assertFalse(it.hasNext());

		// Changing the sendability to 0 should make the message unsendable
		db.setSendability(txn, messageId, 0);
		assertFalse(db.hasSendableMessages(txn, contactId));
		it = db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertFalse(it.hasNext());

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testSendableMessagesMustHaveStatusNew() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		db.addMessage(txn, message);
		db.setSendability(txn, messageId, 1);

		// The message has no status yet, so it should not be sendable
		assertFalse(db.hasSendableMessages(txn, contactId));
		Iterator<MessageId> it =
			db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertFalse(it.hasNext());

		// Changing the status to Status.NEW should make the message sendable
		db.setStatus(txn, contactId, messageId, Status.NEW);
		assertTrue(db.hasSendableMessages(txn, contactId));
		it = db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertTrue(it.hasNext());
		assertEquals(messageId, it.next());
		assertFalse(it.hasNext());

		// Changing the status to SENT should make the message unsendable
		db.setStatus(txn, contactId, messageId, Status.SENT);
		assertFalse(db.hasSendableMessages(txn, contactId));
		it = db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertFalse(it.hasNext());

		// Changing the status to SEEN should also make the message unsendable
		db.setStatus(txn, contactId, messageId, Status.SEEN);
		it = db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertFalse(it.hasNext());

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testSendableMessagesMustBeSubscribed() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		db.addMessage(txn, message);
		db.setSendability(txn, messageId, 1);
		db.setStatus(txn, contactId, messageId, Status.NEW);

		// The contact is not subscribed, so the message should not be sendable
		assertFalse(db.hasSendableMessages(txn, contactId));
		Iterator<MessageId> it =
			db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertFalse(it.hasNext());

		// The contact subscribing should make the message sendable
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		assertTrue(db.hasSendableMessages(txn, contactId));
		it = db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertTrue(it.hasNext());
		assertEquals(messageId, it.next());
		assertFalse(it.hasNext());

		// The contact unsubscribing should make the message unsendable
		db.setSubscriptions(txn, contactId,
				Collections.<Group, Long>emptyMap(), 2);
		assertFalse(db.hasSendableMessages(txn, contactId));
		it = db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertFalse(it.hasNext());

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testSendableMessagesMustBeNewerThanSubscriptions()
	throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		db.addMessage(txn, message);
		db.setSendability(txn, messageId, 1);
		db.setStatus(txn, contactId, messageId, Status.NEW);

		// The message is older than the contact's subscription, so it should
		// not be sendable
		db.setSubscriptions(txn, contactId,
				Collections.singletonMap(group, timestamp + 1), 1);
		assertFalse(db.hasSendableMessages(txn, contactId));
		Iterator<MessageId> it =
			db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertFalse(it.hasNext());

		// Changing the contact's subscription should make the message sendable
		db.setSubscriptions(txn, contactId,
				Collections.singletonMap(group, timestamp), 2);
		assertTrue(db.hasSendableMessages(txn, contactId));
		it = db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertTrue(it.hasNext());
		assertEquals(messageId, it.next());
		assertFalse(it.hasNext());

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testSendableMessagesMustFitCapacity() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		db.addMessage(txn, message);
		db.setSendability(txn, messageId, 1);
		db.setStatus(txn, contactId, messageId, Status.NEW);

		// The message is sendable, but too large to send
		assertTrue(db.hasSendableMessages(txn, contactId));
		Iterator<MessageId> it =
			db.getSendableMessages(txn, contactId, size - 1).iterator();
		assertFalse(it.hasNext());

		// The message is just the right size to send
		assertTrue(db.hasSendableMessages(txn, contactId));
		it = db.getSendableMessages(txn, contactId, size).iterator();
		assertTrue(it.hasNext());
		assertEquals(messageId, it.next());
		assertFalse(it.hasNext());

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testSendableMessagesMustBeVisible() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		db.addMessage(txn, message);
		db.setSendability(txn, messageId, 1);
		db.setStatus(txn, contactId, messageId, Status.NEW);

		// The subscription is not visible to the contact, so the message
		// should not be sendable
		assertFalse(db.hasSendableMessages(txn, contactId));
		Iterator<MessageId> it =
			db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertFalse(it.hasNext());

		// Making the subscription visible should make the message sendable
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		assertTrue(db.hasSendableMessages(txn, contactId));
		it = db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertTrue(it.hasNext());
		assertEquals(messageId, it.next());
		assertFalse(it.hasNext());

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testBatchesToAck() throws DbException {
		BatchId batchId1 = new BatchId(TestUtils.getRandomId());
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact and some batches to ack
		assertEquals(contactId, db.addContact(txn, transports));
		db.addBatchToAck(txn, contactId, batchId);
		db.addBatchToAck(txn, contactId, batchId1);
		db.commitTransaction(txn);

		// Both batch IDs should be returned
		Collection<BatchId> acks = db.getBatchesToAck(txn, contactId);
		assertEquals(2, acks.size());
		assertTrue(acks.contains(batchId));
		assertTrue(acks.contains(batchId1));

		// Remove the batch IDs
		db.removeBatchesToAck(txn, contactId, acks);

		// Both batch IDs should have been removed
		acks = db.getBatchesToAck(txn, contactId);
		assertEquals(0, acks.size());

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testDuplicateBatchesReceived() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact and receive the same batch twice
		assertEquals(contactId, db.addContact(txn, transports));
		db.addBatchToAck(txn, contactId, batchId);
		db.addBatchToAck(txn, contactId, batchId);
		db.commitTransaction(txn);

		// The batch ID should only be returned once
		Collection<BatchId> acks = db.getBatchesToAck(txn, contactId);
		assertEquals(1, acks.size());
		assertTrue(acks.contains(batchId));

		// Remove the batch ID
		db.removeBatchesToAck(txn, contactId, acks);

		// The batch ID should have been removed
		acks = db.getBatchesToAck(txn, contactId);
		assertEquals(0, acks.size());

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testRemoveAckedBatch() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		db.addMessage(txn, message);
		db.setSendability(txn, messageId, 1);
		db.setStatus(txn, contactId, messageId, Status.NEW);

		// Get the message and mark it as sent
		Iterator<MessageId> it =
			db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertTrue(it.hasNext());
		assertEquals(messageId, it.next());
		assertFalse(it.hasNext());
		db.setStatus(txn, contactId, messageId, Status.SENT);
		db.addOutstandingBatch(txn, contactId, batchId,
				Collections.singleton(messageId));

		// The message should no longer be sendable
		it = db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertFalse(it.hasNext());
		// Pretend that the batch was acked
		db.removeAckedBatch(txn, contactId, batchId);
		// The message still should not be sendable
		it = db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertFalse(it.hasNext());

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testRemoveLostBatch() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		db.addMessage(txn, message);
		db.setSendability(txn, messageId, 1);
		db.setStatus(txn, contactId, messageId, Status.NEW);

		// Get the message and mark it as sent
		Iterator<MessageId> it =
			db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertTrue(it.hasNext());
		assertEquals(messageId, it.next());
		assertFalse(it.hasNext());
		db.setStatus(txn, contactId, messageId, Status.SENT);
		db.addOutstandingBatch(txn, contactId, batchId,
				Collections.singleton(messageId));

		// The message should no longer be sendable
		it = db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertFalse(it.hasNext());
		// Pretend that the batch was lost
		db.removeLostBatch(txn, contactId, batchId);
		// The message should be sendable again
		it = db.getSendableMessages(txn, contactId, ONE_MEGABYTE).iterator();
		assertTrue(it.hasNext());
		assertEquals(messageId, it.next());
		assertFalse(it.hasNext());

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testRetransmission() throws DbException {
		BatchId[] ids = new BatchId[Database.RETRANSMIT_THRESHOLD + 5];
		for(int i = 0; i < ids.length; i++) {
			ids[i] = new BatchId(TestUtils.getRandomId());
		}
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact
		assertEquals(contactId, db.addContact(txn, transports));
		// Add some outstanding batches, a few ms apart
		for(int i = 0; i < ids.length; i++) {
			db.addOutstandingBatch(txn, contactId, ids[i],
					Collections.<MessageId>emptySet());
			try {
				Thread.sleep(5);
			} catch(InterruptedException ignored) {}
		}
		// The contact acks the batches in reverse order. The first
		// RETRANSMIT_THRESHOLD - 1 acks should not trigger any retransmissions
		for(int i = 0; i < Database.RETRANSMIT_THRESHOLD - 1; i++) {
			db.removeAckedBatch(txn, contactId, ids[ids.length - i - 1]);
			Collection<BatchId> lost = db.getLostBatches(txn, contactId);
			assertEquals(Collections.emptyList(), lost);
		}
		// The next ack should trigger the retransmission of the remaining
		// five outstanding batches
		int index = ids.length - Database.RETRANSMIT_THRESHOLD;
		db.removeAckedBatch(txn, contactId, ids[index]);
		Collection<BatchId> lost = db.getLostBatches(txn, contactId);
		for(int i = 0; i < index; i++) {
			assertTrue(lost.contains(ids[i]));
		}

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testNoRetransmission() throws DbException {
		BatchId[] ids = new BatchId[Database.RETRANSMIT_THRESHOLD * 2];
		for(int i = 0; i < ids.length; i++) {
			ids[i] = new BatchId(TestUtils.getRandomId());
		}
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact
		assertEquals(contactId, db.addContact(txn, transports));
		// Add some outstanding batches, a few ms apart
		for(int i = 0; i < ids.length; i++) {
			db.addOutstandingBatch(txn, contactId, ids[i],
					Collections.<MessageId>emptySet());
			try {
				Thread.sleep(5);
			} catch(InterruptedException ignored) {}
		}
		// The contact acks the batches in the order they were sent - nothing
		// should be retransmitted
		for(int i = 0; i < ids.length; i++) {
			db.removeAckedBatch(txn, contactId, ids[i]);
			Collection<BatchId> lost = db.getLostBatches(txn, contactId);
			assertEquals(Collections.emptyList(), lost);
		}

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testGetMessagesByAuthor() throws DbException {
		AuthorId authorId1 = new AuthorId(TestUtils.getRandomId());
		MessageId messageId1 = new MessageId(TestUtils.getRandomId());
		Message message1 = new TestMessage(messageId1, MessageId.NONE, groupId,
				authorId1, timestamp, raw);
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Subscribe to a group and store two messages
		db.addSubscription(txn, group);
		db.addMessage(txn, message);
		db.addMessage(txn, message1);

		// Check that each message is retrievable via its author
		Iterator<MessageId> it =
			db.getMessagesByAuthor(txn, authorId).iterator();
		assertTrue(it.hasNext());
		assertEquals(messageId, it.next());
		assertFalse(it.hasNext());
		it = db.getMessagesByAuthor(txn, authorId1).iterator();
		assertTrue(it.hasNext());
		assertEquals(messageId1, it.next());
		assertFalse(it.hasNext());

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testGetNumberOfSendableChildren() throws DbException {
		MessageId childId1 = new MessageId(TestUtils.getRandomId());
		MessageId childId2 = new MessageId(TestUtils.getRandomId());
		MessageId childId3 = new MessageId(TestUtils.getRandomId());
		GroupId groupId1 = new GroupId(TestUtils.getRandomId());
		Group group1 = groupFactory.createGroup(groupId1, "Another group name",
				null);
		Message child1 = new TestMessage(childId1, messageId, groupId,
				authorId, timestamp, raw);
		Message child2 = new TestMessage(childId2, messageId, groupId,
				authorId, timestamp, raw);
		// The third child is in a different group
		Message child3 = new TestMessage(childId3, messageId, groupId1,
				authorId, timestamp, raw);
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Subscribe to the groups and store the messages
		db.addSubscription(txn, group);
		db.addSubscription(txn, group1);
		db.addMessage(txn, message);
		db.addMessage(txn, child1);
		db.addMessage(txn, child2);
		db.addMessage(txn, child3);
		// Make all the children sendable
		db.setSendability(txn, childId1, 1);
		db.setSendability(txn, childId2, 5);
		db.setSendability(txn, childId3, 3);

		// There should be two sendable children
		assertEquals(2, db.getNumberOfSendableChildren(txn, messageId));
		// Make one of the children unsendable
		db.setSendability(txn, childId1, 0);
		// Now there should be one sendable child
		assertEquals(1, db.getNumberOfSendableChildren(txn, messageId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testGetOldMessages() throws DbException {
		MessageId messageId1 = new MessageId(TestUtils.getRandomId());
		Message message1 = new TestMessage(messageId1, MessageId.NONE, groupId,
				authorId, timestamp + 1000, raw);
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Subscribe to a group and store two messages
		db.addSubscription(txn, group);
		db.addMessage(txn, message);
		db.addMessage(txn, message1);

		// Allowing enough capacity for one message should return the older one
		Iterator<MessageId> it = db.getOldMessages(txn, size).iterator();
		assertTrue(it.hasNext());
		assertEquals(messageId, it.next());
		assertFalse(it.hasNext());

		// Allowing enough capacity for both messages should return both
		Collection<MessageId> ids = new HashSet<MessageId>();
		for(MessageId id : db.getOldMessages(txn, size * 2)) ids.add(id);
		assertEquals(2, ids.size());
		assertTrue(ids.contains(messageId));
		assertTrue(ids.contains(messageId1));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testGetFreeSpace() throws Exception {
		byte[] largeBody = new byte[ONE_MEGABYTE];
		for(int i = 0; i < largeBody.length; i++) largeBody[i] = (byte) i;
		Message message1 = new TestMessage(messageId, MessageId.NONE, groupId,
				authorId, timestamp, largeBody);
		Database<Connection> db = open(false);

		// Sanity check: there should be enough space on disk for this test
		String path = testDir.getAbsolutePath();
		assertTrue(FileSystemUtils.freeSpaceKb(path) * 1024L > MAX_SIZE);
		// The free space should not be more than the allowed maximum size
		long free = db.getFreeSpace();
		assertTrue(free <= MAX_SIZE);
		assertTrue(free > 0);
		// Storing a message should reduce the free space
		Connection txn = db.startTransaction();
		db.addSubscription(txn, group);
		db.addMessage(txn, message1);
		db.commitTransaction(txn);
		assertTrue(db.getFreeSpace() < free);

		db.close();
	}

	@Test
	public void testCloseWaitsForCommit() throws DbException {
		final AtomicBoolean transactionFinished = new AtomicBoolean(false);
		final AtomicBoolean closed = new AtomicBoolean(false);
		final AtomicBoolean error = new AtomicBoolean(false);
		final Database<Connection> db = open(false);

		// Start a transaction
		Connection txn = db.startTransaction();
		// In another thread, close the database
		Thread t = new Thread() {
			public void run() {
				try {
					db.close();
					closed.set(true);
					if(!transactionFinished.get()) error.set(true);
				} catch(DbException e) {
					error.set(true);
				}
			}
		};
		t.start();
		// Do whatever the transaction needs to do
		try {
			Thread.sleep(100);
		} catch(InterruptedException ignored) {}
		transactionFinished.set(true);
		// Commit the transaction
		db.commitTransaction(txn);
		// The other thread should now terminate
		try {
			t.join(10 * 1000);
		} catch(InterruptedException ignored) {}
		assertTrue(closed.get());
		// Check that the other thread didn't encounter an error
		assertFalse(error.get());
	}

	@Test
	public void testCloseWaitsForAbort() throws DbException {
		final AtomicBoolean transactionFinished = new AtomicBoolean(false);
		final AtomicBoolean closed = new AtomicBoolean(false);
		final AtomicBoolean error = new AtomicBoolean(false);
		final Database<Connection> db = open(false);

		// Start a transaction
		Connection txn = db.startTransaction();
		// In another thread, close the database
		Thread t = new Thread() {
			public void run() {
				try {
					db.close();
					closed.set(true);
					if(!transactionFinished.get()) error.set(true);
				} catch(DbException e) {
					error.set(true);
				}
			}
		};
		t.start();
		// Do whatever the transaction needs to do
		try {
			Thread.sleep(100);
		} catch(InterruptedException ignored) {}
		transactionFinished.set(true);
		// Abort the transaction
		db.abortTransaction(txn);
		// The other thread should now terminate
		try {
			t.join(10000);
		} catch(InterruptedException ignored) {}
		assertTrue(closed.get());
		// Check that the other thread didn't encounter an error
		assertFalse(error.get());
	}

	@Test
	public void testUpdateTransportPropertiess() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact with some transport properties
		assertEquals(contactId, db.addContact(txn, transports));
		assertEquals(transports, db.getTransports(txn, contactId));
		// Replace the transport properties
		Map<String, Map<String, String>> transports1 =
			new TreeMap<String, Map<String, String>>();
		transports1.put("foo", Collections.singletonMap("bar", "baz"));
		transports1.put("bar", Collections.singletonMap("baz", "quux"));
		db.setTransports(txn, contactId, transports1, 1);
		assertEquals(transports1, db.getTransports(txn, contactId));
		// Remove the transport properties
		db.setTransports(txn, contactId,
				Collections.<String, Map<String, String>>emptyMap(), 2);
		assertEquals(Collections.emptyMap(), db.getTransports(txn, contactId));
		// Set the local transport properties
		for(String name : transports.keySet()) {
			db.setTransportProperties(txn, name, transports.get(name));
		}
		assertEquals(transports, db.getTransports(txn));
		// Remove the local transport properties
		for(String name : transports.keySet()) {
			db.setTransportProperties(txn, name,
					Collections.<String, String>emptyMap());
		}
		assertEquals(Collections.emptyMap(), db.getTransports(txn));

		db.commitTransaction(txn);
		db.close();
	}


	@Test
	public void testUpdateTransportConfig() throws DbException {
		Map<String, String> config = Collections.singletonMap("bar", "baz");
		Map<String, String> config1 = Collections.singletonMap("baz", "bam");
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Set the transport config
		db.setTransportConfig(txn, "foo", config);
		assertEquals(config, db.getTransportConfig(txn, "foo"));
		// Update the transport config
		db.setTransportConfig(txn, "foo", config1);
		assertEquals(config1, db.getTransportConfig(txn, "foo"));
		// Remove the transport config
		db.setTransportConfig(txn, "foo",
				Collections.<String, String>emptyMap());
		assertEquals(Collections.emptyMap(), db.getTransportConfig(txn, "foo"));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testTransportsNotUpdatedIfTimestampIsOld() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact with some transport properties
		assertEquals(contactId, db.addContact(txn, transports));
		assertEquals(transports, db.getTransports(txn, contactId));
		// Replace the transport properties using a timestamp of 2
		Map<String, Map<String, String>> transports1 =
			new TreeMap<String, Map<String, String>>();
		transports1.put("foo", Collections.singletonMap("bar", "baz"));
		transports1.put("bar", Collections.singletonMap("baz", "quux"));
		db.setTransports(txn, contactId, transports1, 2);
		assertEquals(transports1, db.getTransports(txn, contactId));
		// Try to replace the transport properties using a timestamp of 1
		Map<String, Map<String, String>> transports2 =
			new TreeMap<String, Map<String, String>>();
		transports2.put("bar", Collections.singletonMap("baz", "quux"));
		transports2.put("baz", Collections.singletonMap("quux", "fnord"));
		db.setTransports(txn, contactId, transports2, 1);
		// The old properties should still be there
		assertEquals(transports1, db.getTransports(txn, contactId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testUpdateSubscriptions() throws DbException {
		GroupId groupId1 = new GroupId(TestUtils.getRandomId());
		Group group1 = groupFactory.createGroup(groupId1, "Another group name",
				null);
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact
		assertEquals(contactId, db.addContact(txn, transports));
		// Add some subscriptions
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		assertEquals(Collections.singletonList(group),
				db.getSubscriptions(txn, contactId));
		// Update the subscriptions
		Map<Group, Long> subscriptions1 = Collections.singletonMap(group1, 0L);
		db.setSubscriptions(txn, contactId, subscriptions1, 2);
		assertEquals(Collections.singletonList(group1),
				db.getSubscriptions(txn, contactId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testSubscriptionsNotUpdatedIfTimestampIsOld()
	throws DbException {
		GroupId groupId1 = new GroupId(TestUtils.getRandomId());
		Group group1 = groupFactory.createGroup(groupId1, "Another group name",
				null);
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact
		assertEquals(contactId, db.addContact(txn, transports));
		// Add some subscriptions
		db.setSubscriptions(txn, contactId, subscriptions, 2);
		assertEquals(Collections.singletonList(group),
				db.getSubscriptions(txn, contactId));
		// Try to update the subscriptions using a timestamp of 1
		Map<Group, Long> subscriptions1 = Collections.singletonMap(group1, 0L);
		db.setSubscriptions(txn, contactId, subscriptions1, 1);
		// The old subscriptions should still be there
		assertEquals(Collections.singletonList(group),
				db.getSubscriptions(txn, contactId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testGetMessageIfSendableReturnsNullIfNotInDatabase()
	throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact and subscribe to a group
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setSubscriptions(txn, contactId, subscriptions, 1);

		// The message is not in the database
		assertNull(db.getMessageIfSendable(txn, contactId, messageId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testGetMessageIfSendableReturnsNullIfSeen()
	throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		db.addMessage(txn, message);
		// Set the sendability to > 0
		db.setSendability(txn, messageId, 1);
		// Set the status to SEEN
		db.setStatus(txn, contactId, messageId, Status.SEEN);

		// The message is not sendable because its status is SEEN
		assertNull(db.getMessageIfSendable(txn, contactId, messageId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testGetMessageIfSendableReturnsNullIfNotSendable()
	throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		db.addMessage(txn, message);
		// Set the sendability to 0
		db.setSendability(txn, messageId, 0);
		// Set the status to NEW
		db.setStatus(txn, contactId, messageId, Status.NEW);

		// The message is not sendable because its sendability is 0
		assertNull(db.getMessageIfSendable(txn, contactId, messageId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testGetMessageIfSendableReturnsNullIfOld() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		// The message is older than the contact's subscription
		Map<Group, Long> subs = Collections.singletonMap(group, timestamp + 1);
		db.setSubscriptions(txn, contactId, subs, 1);
		db.addMessage(txn, message);
		// Set the sendability to > 0
		db.setSendability(txn, messageId, 1);
		// Set the status to NEW
		db.setStatus(txn, contactId, messageId, Status.NEW);

		// The message is not sendable because it's too old
		assertNull(db.getMessageIfSendable(txn, contactId, messageId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testGetMessageIfSendableReturnsMessage() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		db.addMessage(txn, message);
		// Set the sendability to > 0
		db.setSendability(txn, messageId, 1);
		// Set the status to NEW
		db.setStatus(txn, contactId, messageId, Status.NEW);

		// The message is sendable so it should be returned
		byte[] b = db.getMessageIfSendable(txn, contactId, messageId);
		assertTrue(Arrays.equals(raw, b));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testSetStatusSeenIfVisibleRequiresMessageInDatabase()
	throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact and subscribe to a group
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		db.setSubscriptions(txn, contactId, subscriptions, 1);

		// The message is not in the database
		assertFalse(db.setStatusSeenIfVisible(txn, contactId, messageId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testSetStatusSeenIfVisibleRequiresLocalSubscription()
	throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact with a subscription
		assertEquals(contactId, db.addContact(txn, transports));
		db.setSubscriptions(txn, contactId, subscriptions, 1);

		// There's no local subscription for the group
		assertFalse(db.setStatusSeenIfVisible(txn, contactId, messageId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testSetStatusSeenIfVisibleRequiresContactSubscription()
	throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.addMessage(txn, message);
		db.setStatus(txn, contactId, messageId, Status.NEW);

		// There's no contact subscription for the group
		assertFalse(db.setStatusSeenIfVisible(txn, contactId, messageId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testSetStatusSeenIfVisibleRequiresVisibility()
	throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.addMessage(txn, message);
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		db.setStatus(txn, contactId, messageId, Status.NEW);

		// The subscription is not visible
		assertFalse(db.setStatusSeenIfVisible(txn, contactId, messageId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testSetStatusSeenIfVisibleReturnsTrueIfAlreadySeen()
	throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		db.addMessage(txn, message);
		// The message has already been seen by the contact
		db.setStatus(txn, contactId, messageId, Status.SEEN);

		assertTrue(db.setStatusSeenIfVisible(txn, contactId, messageId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testSetStatusSeenIfVisibleReturnsTrueIfNew()
	throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact, subscribe to a group and store a message
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		db.setSubscriptions(txn, contactId, subscriptions, 1);
		db.addMessage(txn, message);
		// The message has not been seen by the contact
		db.setStatus(txn, contactId, messageId, Status.NEW);

		assertTrue(db.setStatusSeenIfVisible(txn, contactId, messageId));

		db.commitTransaction(txn);
		db.close();
	}

	@Test
	public void testVisibility() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();

		// Add a contact and subscribe to a group
		assertEquals(contactId, db.addContact(txn, transports));
		db.addSubscription(txn, group);

		// The group should not be visible to the contact
		assertEquals(Collections.emptyList(), db.getVisibility(txn, groupId));

		// Make the group visible to the contact
		db.setVisibility(txn, groupId, Collections.singleton(contactId));
		assertEquals(Collections.singletonList(contactId),
				db.getVisibility(txn, groupId));

		// Make the group invisible again
		db.setVisibility(txn, groupId, Collections.<ContactId>emptySet());
		assertEquals(Collections.emptyList(), db.getVisibility(txn, groupId));
	}

	@Test
	public void testExceptionHandling() throws DbException {
		Database<Connection> db = open(false);
		Connection txn = db.startTransaction();
		try {
			// Ask for a nonexistent message - an exception should be thrown
			db.getMessage(txn, messageId);
			fail();
		} catch(DbException expected) {
			// It should be possible to abort the transaction without error
			db.abortTransaction(txn);
		}
		// It should be possible to close the database cleanly
		db.close();
	}

	private Database<Connection> open(boolean resume) throws DbException {
		Database<Connection> db = new H2Database(testDir, password, MAX_SIZE,
				groupFactory);
		db.open(resume);
		return db;
	}

	@After
	public void tearDown() {
		TestUtils.deleteTestDirectory(testDir);
	}

	private class TestPassword implements Password {

		public char[] getPassword() {
			return passwordString.toCharArray();
		}
	}
}

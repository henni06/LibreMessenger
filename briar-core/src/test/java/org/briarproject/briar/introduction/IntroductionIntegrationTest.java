package org.briarproject.briar.introduction;

import net.jodah.concurrentunit.Waiter;

import org.briarproject.bramble.api.FormatException;
import org.briarproject.bramble.api.client.ClientHelper;
import org.briarproject.bramble.api.contact.Contact;
import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.data.BdfDictionary;
import org.briarproject.bramble.api.data.BdfEntry;
import org.briarproject.bramble.api.data.BdfList;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.Transaction;
import org.briarproject.bramble.api.event.Event;
import org.briarproject.bramble.api.event.EventListener;
import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.bramble.api.properties.TransportProperties;
import org.briarproject.bramble.api.properties.TransportPropertyManager;
import org.briarproject.bramble.api.sync.Group;
import org.briarproject.bramble.api.sync.Message;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.bramble.test.TestDatabaseModule;
import org.briarproject.briar.api.client.ProtocolStateException;
import org.briarproject.briar.api.client.SessionId;
import org.briarproject.briar.api.introduction.IntroductionManager;
import org.briarproject.briar.api.introduction.IntroductionMessage;
import org.briarproject.briar.api.introduction.IntroductionRequest;
import org.briarproject.briar.api.introduction.event.IntroductionAbortedEvent;
import org.briarproject.briar.api.introduction.event.IntroductionRequestReceivedEvent;
import org.briarproject.briar.api.introduction.event.IntroductionResponseReceivedEvent;
import org.briarproject.briar.api.introduction.event.IntroductionSucceededEvent;
import org.briarproject.briar.test.BriarIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static org.briarproject.bramble.api.identity.AuthorConstants.MAX_PUBLIC_KEY_LENGTH;
import static org.briarproject.bramble.test.TestPluginConfigModule.TRANSPORT_ID;
import static org.briarproject.bramble.test.TestUtils.getRandomBytes;
import static org.briarproject.bramble.test.TestUtils.getTransportId;
import static org.briarproject.bramble.test.TestUtils.getTransportProperties;
import static org.briarproject.bramble.test.TestUtils.getTransportPropertiesMap;
import static org.briarproject.briar.api.introduction.IntroductionManager.CLIENT_ID;
import static org.briarproject.briar.api.introduction.IntroductionManager.CLIENT_VERSION;
import static org.briarproject.briar.introduction.IntroductionConstants.MSG_KEY_MESSAGE_TYPE;
import static org.briarproject.briar.introduction.IntroductionConstants.SESSION_KEY_AUTHOR;
import static org.briarproject.briar.introduction.IntroductionConstants.SESSION_KEY_INTRODUCEE_1;
import static org.briarproject.briar.introduction.IntroductionConstants.SESSION_KEY_INTRODUCEE_2;
import static org.briarproject.briar.introduction.IntroductionConstants.SESSION_KEY_LAST_LOCAL_MESSAGE_ID;
import static org.briarproject.briar.introduction.IntroductionConstants.SESSION_KEY_SESSION_ID;
import static org.briarproject.briar.introduction.MessageType.ACCEPT;
import static org.briarproject.briar.test.BriarTestUtils.assertGroupCount;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IntroductionIntegrationTest
		extends BriarIntegrationTest<IntroductionIntegrationTestComponent> {

	// objects accessed from background threads need to be volatile
	private volatile IntroductionManager introductionManager0;
	private volatile IntroductionManager introductionManager1;
	private volatile IntroductionManager introductionManager2;
	private volatile Waiter eventWaiter;

	private IntroducerListener listener0;
	private IntroduceeListener listener1;
	private IntroduceeListener listener2;

	private static final Logger LOG =
			Logger.getLogger(IntroductionIntegrationTest.class.getName());

	interface StateVisitor {
		AcceptMessage visit(AcceptMessage response);
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		introductionManager0 = c0.getIntroductionManager();
		introductionManager1 = c1.getIntroductionManager();
		introductionManager2 = c2.getIntroductionManager();

		// initialize waiter fresh for each test
		eventWaiter = new Waiter();

		addTransportProperties();
	}

	@Override
	protected void createComponents() {
		IntroductionIntegrationTestComponent component =
				DaggerIntroductionIntegrationTestComponent.builder().build();
		component.inject(this);

		c0 = DaggerIntroductionIntegrationTestComponent.builder()
				.testDatabaseModule(new TestDatabaseModule(t0Dir)).build();
		injectEagerSingletons(c0);

		c1 = DaggerIntroductionIntegrationTestComponent.builder()
				.testDatabaseModule(new TestDatabaseModule(t1Dir)).build();
		injectEagerSingletons(c1);

		c2 = DaggerIntroductionIntegrationTestComponent.builder()
				.testDatabaseModule(new TestDatabaseModule(t2Dir)).build();
		injectEagerSingletons(c2);
	}

	@Test
	public void testIntroductionSession() throws Exception {
		addListeners(true, true);

		// make introduction
		long time = clock.currentTimeMillis();
		Contact introducee1 = contact1From0;
		Contact introducee2 = contact2From0;
		introductionManager0
				.makeIntroduction(introducee1, introducee2, "Hi!", time);

		// check that messages are tracked properly
		Group g1 = introductionManager0.getContactGroup(introducee1);
		Group g2 = introductionManager0.getContactGroup(introducee2);
		assertGroupCount(messageTracker0, g1.getId(), 1, 0);
		assertGroupCount(messageTracker0, g2.getId(), 1, 0);

		// sync first REQUEST message
		sync0To1(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener1.requestReceived);
		assertGroupCount(messageTracker1, g1.getId(), 2, 1);

		// sync second REQUEST message
		sync0To2(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener2.requestReceived);
		assertGroupCount(messageTracker2, g2.getId(), 2, 1);

		// sync first ACCEPT message
		sync1To0(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener0.response1Received);
		assertGroupCount(messageTracker0, g1.getId(), 2, 1);

		// sync second ACCEPT message
		sync2To0(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener0.response2Received);
		assertGroupCount(messageTracker0, g2.getId(), 2, 1);

		// sync forwarded ACCEPT messages to introducees
		sync0To1(1, true);
		sync0To2(1, true);

		// sync first AUTH and its forward
		sync1To0(1, true);
		sync0To2(1, true);

		// sync second AUTH and its forward as well as the following ACTIVATE
		sync2To0(2, true);
		sync0To1(2, true);

		// sync first ACTIVATE and its forward
		sync1To0(1, true);
		sync0To2(1, true);

		// wait for introduction to succeed
		eventWaiter.await(TIMEOUT, 2);
		assertTrue(listener1.succeeded);
		assertTrue(listener2.succeeded);

		assertTrue(contactManager1
				.contactExists(author2.getId(), author1.getId()));
		assertTrue(contactManager2
				.contactExists(author1.getId(), author2.getId()));

		// make sure that introduced contacts are not verified
		for (Contact c : contactManager1.getActiveContacts()) {
			if (c.getAuthor().equals(author2)) {
				assertFalse(c.isVerified());
			}
		}
		for (Contact c : contactManager2.getActiveContacts()) {
			if (c.getAuthor().equals(author1)) {
				assertFalse(c.isVerified());
			}
		}

		assertDefaultUiMessages();
		assertGroupCount(messageTracker0, g1.getId(), 2, 1);
		assertGroupCount(messageTracker0, g2.getId(), 2, 1);
		assertGroupCount(messageTracker1, g1.getId(), 2, 1);
		assertGroupCount(messageTracker2, g2.getId(), 2, 1);
	}

	@Test
	public void testIntroductionSessionFirstDecline() throws Exception {
		addListeners(false, true);

		// make introduction
		long time = clock.currentTimeMillis();
		Contact introducee1 = contact1From0;
		Contact introducee2 = contact2From0;
		introductionManager0
				.makeIntroduction(introducee1, introducee2, null, time);

		// sync request messages
		sync0To1(1, true);
		sync0To2(1, true);

		// wait for requests to arrive
		eventWaiter.await(TIMEOUT, 2);
		assertTrue(listener1.requestReceived);
		assertTrue(listener2.requestReceived);

		// sync first response
		sync1To0(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener0.response1Received);

		// sync second response
		sync2To0(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener0.response2Received);

		// sync first forwarded response
		sync0To2(1, true);

		// note how the introducer does not forward the second response,
		// because after the first decline the protocol finished

		assertFalse(listener1.succeeded);
		assertFalse(listener2.succeeded);

		assertFalse(contactManager1
				.contactExists(author2.getId(), author1.getId()));
		assertFalse(contactManager2
				.contactExists(author1.getId(), author2.getId()));

		Group g1 = introductionManager0.getContactGroup(introducee1);
		Group g2 = introductionManager0.getContactGroup(introducee2);
		assertEquals(2,
				introductionManager0.getIntroductionMessages(contactId1From0)
						.size());
		assertGroupCount(messageTracker0, g1.getId(), 2, 1);
		assertEquals(2,
				introductionManager0.getIntroductionMessages(contactId2From0)
						.size());
		assertGroupCount(messageTracker0, g2.getId(), 2, 1);
		assertEquals(2,
				introductionManager1.getIntroductionMessages(contactId0From1)
						.size());
		assertGroupCount(messageTracker1, g1.getId(), 2, 1);
		// introducee2 should also have the decline response of introducee1
		assertEquals(3,
				introductionManager2.getIntroductionMessages(contactId0From2)
						.size());
		assertGroupCount(messageTracker2, g2.getId(), 3, 2);

		assertFalse(listener0.aborted);
		assertFalse(listener1.aborted);
		assertFalse(listener2.aborted);
	}

	@Test
	public void testIntroductionSessionSecondDecline() throws Exception {
		addListeners(true, false);

		// make introduction
		long time = clock.currentTimeMillis();
		introductionManager0
				.makeIntroduction(contact1From0, contact2From0, null, time);

		// sync request messages
		sync0To1(1, true);
		sync0To2(1, true);

		// wait for requests to arrive
		eventWaiter.await(TIMEOUT, 2);
		assertTrue(listener1.requestReceived);
		assertTrue(listener2.requestReceived);

		// sync first response
		sync1To0(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener0.response1Received);

		// sync second response
		sync2To0(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener0.response2Received);

		// sync both forwarded response
		sync0To2(1, true);
		sync0To1(1, true);

		assertFalse(contactManager1
				.contactExists(author2.getId(), author1.getId()));
		assertFalse(contactManager2
				.contactExists(author1.getId(), author2.getId()));

		assertEquals(2,
				introductionManager0.getIntroductionMessages(contactId1From0)
						.size());
		assertEquals(2,
				introductionManager0.getIntroductionMessages(contactId2From0)
						.size());
		// introducee1 also sees the decline response from introducee2
		assertEquals(3,
				introductionManager1.getIntroductionMessages(contactId0From1)
						.size());
		assertEquals(2,
				introductionManager2.getIntroductionMessages(contactId0From2)
						.size());
		assertFalse(listener0.aborted);
		assertFalse(listener1.aborted);
		assertFalse(listener2.aborted);
	}

	@Test
	public void testIntroductionSessionDelayedFirstDecline() throws Exception {
		addListeners(false, false);

		// make introduction
		long time = clock.currentTimeMillis();
		introductionManager0
				.makeIntroduction(contact1From0, contact2From0, null, time);

		// sync request messages
		sync0To1(1, true);
		sync0To2(1, true);

		// wait for requests to arrive
		eventWaiter.await(TIMEOUT, 2);
		assertTrue(listener1.requestReceived);
		assertTrue(listener2.requestReceived);

		// sync first response
		sync1To0(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener0.response1Received);

		// sync fake transport properties back to 1, so Message ACK can arrive
		// and the assertDefaultUiMessages() check at the end will not fail
		TransportProperties tp = new TransportProperties(
				Collections.singletonMap("key", "value"));
		c0.getTransportPropertyManager()
				.mergeLocalProperties(getTransportId(), tp);
		sync0To1(1, true);

		// sync second response
		sync2To0(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener0.response2Received);

		// sync first forwarded response
		sync0To2(1, true);

		// note how the second response will not be forwarded anymore

		assertFalse(contactManager1
				.contactExists(author2.getId(), author1.getId()));
		assertFalse(contactManager2
				.contactExists(author1.getId(), author2.getId()));

		// since introducee2 was already in FINISHED state when
		// introducee1's response arrived, she ignores and deletes it
		assertDefaultUiMessages();
		assertFalse(listener0.aborted);
		assertFalse(listener1.aborted);
		assertFalse(listener2.aborted);
	}

	@Test
	public void testResponseAndAckInOneSession() throws Exception {
		addListeners(true, true);

		// make introduction
		long time = clock.currentTimeMillis();
		introductionManager0
				.makeIntroduction(contact1From0, contact2From0, "Hi!", time);

		// sync first request message
		sync0To1(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener1.requestReceived);

		// sync first response
		sync1To0(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener0.response1Received);

		// don't let 2 answer the request right away
		// to have the response arrive first
		listener2.answerRequests = false;

		// sync second request message and first response
		sync0To2(2, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener2.requestReceived);

		// answer request manually
		introductionManager2
				.acceptIntroduction(contactId0From2, listener2.sessionId, time);

		// sync second response and ACK and make sure there is no abort
		sync2To0(2, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener0.response2Received);
		assertFalse(listener0.aborted);
		assertFalse(listener1.aborted);
		assertFalse(listener2.aborted);
	}

	@Test
	public void testIntroductionToSameContact() throws Exception {
		addListeners(true, false);

		// make introduction
		long time = clock.currentTimeMillis();
		introductionManager0
				.makeIntroduction(contact1From0, contact1From0, null, time);

		// sync request messages
		sync0To1(1, false);

		// we should not get any event, because the request will be discarded
		assertFalse(listener1.requestReceived);

		// make really sure we don't have that request
		assertTrue(introductionManager1.getIntroductionMessages(contactId0From1)
				.isEmpty());

		// The message was invalid, so no abort message was sent
		assertFalse(listener0.aborted);
		assertFalse(listener1.aborted);
		assertFalse(listener2.aborted);
	}

	@Test(expected = ProtocolStateException.class)
	public void testDoubleIntroduction() throws Exception {
		// we can make an introduction
		assertTrue(introductionManager0
				.canIntroduce(contact1From0, contact2From0));

		// make the introduction
		long time = clock.currentTimeMillis();
		introductionManager0
				.makeIntroduction(contact1From0, contact2From0, null, time);

		// no more introduction allowed while the existing one is in progress
		assertFalse(introductionManager0
				.canIntroduce(contact1From0, contact2From0));

		// try it anyway and fail
		introductionManager0
				.makeIntroduction(contact1From0, contact2From0, null, time);
	}

	@Test
	public void testIntroducerRemovedCleanup() throws Exception {
		addListeners(true, true);

		// make introduction
		long time = clock.currentTimeMillis();
		introductionManager0
				.makeIntroduction(contact1From0, contact2From0, "Hi!", time);

		// sync first request message
		sync0To1(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener1.requestReceived);

		// get local group for introducee1
		Group group1 =
				contactGroupFactory.createLocalGroup(CLIENT_ID, CLIENT_VERSION);

		// check that we have one session state
		assertEquals(1, c1.getClientHelper()
				.getMessageMetadataAsDictionary(group1.getId()).size());

		// introducee1 removes introducer
		contactManager1.removeContact(contactId0From1);

		// make sure local state got deleted
		assertEquals(0, c1.getClientHelper()
				.getMessageMetadataAsDictionary(group1.getId()).size());
	}

	@Test
	public void testIntroduceesRemovedCleanup() throws Exception {
		addListeners(true, true);

		// make introduction
		long time = clock.currentTimeMillis();
		introductionManager0
				.makeIntroduction(contact1From0, contact2From0, "Hi!", time);

		// sync first request message
		sync0To1(1, true);
		eventWaiter.await(TIMEOUT, 1);
		assertTrue(listener1.requestReceived);

		// get local group for introducer
		Group group0 =
				contactGroupFactory.createLocalGroup(CLIENT_ID, CLIENT_VERSION);

		// check that we have one session state
		assertEquals(1, c0.getClientHelper()
				.getMessageMetadataAsDictionary(group0.getId()).size());

		// introducer removes introducee1
		contactManager0.removeContact(contactId1From0);

		// make sure local state is still there
		assertEquals(1, c0.getClientHelper()
				.getMessageMetadataAsDictionary(group0.getId()).size());

		// ensure introducer has aborted the session
		assertTrue(listener0.aborted);

		// sync REQUEST and ABORT message
		sync0To2(2, true);

		// ensure introducee2 has aborted the session as well
		assertTrue(listener2.aborted);

		// introducer removes other introducee
		contactManager0.removeContact(contactId2From0);

		// make sure local state is gone now
		assertEquals(0, c0.getClientHelper()
				.getMessageMetadataAsDictionary(group0.getId()).size());
	}

	private void testModifiedResponse(StateVisitor visitor)
			throws Exception {
		addListeners(true, true);

		// make introduction
		long time = clock.currentTimeMillis();
		introductionManager0
				.makeIntroduction(contact1From0, contact2From0, "Hi!", time);

		// sync request messages
		sync0To1(1, true);
		sync0To2(1, true);
		eventWaiter.await(TIMEOUT, 2);

		// sync first response
		sync1To0(1, true);
		eventWaiter.await(TIMEOUT, 1);

		// get response to be forwarded
		AcceptMessage message =
				(AcceptMessage) getMessageFor(c0.getClientHelper(),
						contact2From0, ACCEPT);

		// allow visitor to modify response
		AcceptMessage m = visitor.visit(message);

		// replace original response with modified one
		Transaction txn = db0.startTransaction(false);
		try {
			db0.removeMessage(txn, message.getMessageId());
			Message msg = c0.getMessageEncoder()
					.encodeAcceptMessage(m.getGroupId(), m.getTimestamp(),
							m.getPreviousMessageId(), m.getSessionId(),
							m.getEphemeralPublicKey(), m.getAcceptTimestamp(),
							m.getTransportProperties());
			c0.getClientHelper()
					.addLocalMessage(txn, msg, new BdfDictionary(), true);
			Group group0 = contactGroupFactory
					.createLocalGroup(CLIENT_ID, CLIENT_VERSION);
			BdfDictionary query = BdfDictionary.of(
					new BdfEntry(SESSION_KEY_SESSION_ID, m.getSessionId())
			);
			Map.Entry<MessageId, BdfDictionary> session = c0.getClientHelper()
					.getMessageMetadataAsDictionary(txn, group0.getId(), query)
					.entrySet().iterator().next();
			replacePreviousLocalMessageId(contact2From0.getAuthor(),
					session.getValue(), msg.getId());
			c0.getClientHelper().mergeMessageMetadata(txn, session.getKey(),
					session.getValue());
			db0.commitTransaction(txn);
		} finally {
			db0.endTransaction(txn);
		}

		// sync second response
		sync2To0(1, true);
		eventWaiter.await(TIMEOUT, 1);

		// sync forwarded responses to introducees
		sync0To1(1, true);
		sync0To2(1, true);

		// sync first AUTH and forward it
		sync1To0(1, true);
		sync0To2(1, true);

		// introducee2 should have detected the fake now
		assertFalse(listener0.aborted);
		assertFalse(listener1.aborted);
		assertTrue(listener2.aborted);

		// sync introducee2's ack and following abort
		sync2To0(2, true);

		// ensure introducer got the abort
		assertTrue(listener0.aborted);

		// sync abort messages to introducees
		sync0To1(2, true);

		// ensure everybody got the abort now
		assertTrue(listener0.aborted);
		assertTrue(listener1.aborted);
		assertTrue(listener2.aborted);
	}

	@Test
	public void testModifiedTransportProperties() throws Exception {
		testModifiedResponse(
				m -> new AcceptMessage(m.getMessageId(), m.getGroupId(),
						m.getTimestamp(), m.getPreviousMessageId(),
						m.getSessionId(), m.getEphemeralPublicKey(),
						m.getAcceptTimestamp(),
						getTransportPropertiesMap(2))
		);
	}

	@Test
	public void testModifiedTimestamp() throws Exception {
		testModifiedResponse(
				m -> new AcceptMessage(m.getMessageId(), m.getGroupId(),
						m.getTimestamp(), m.getPreviousMessageId(),
						m.getSessionId(), m.getEphemeralPublicKey(),
						clock.currentTimeMillis(),
						m.getTransportProperties())
		);
	}

	@Test
	public void testModifiedEphemeralPublicKey() throws Exception {
		testModifiedResponse(
				m -> new AcceptMessage(m.getMessageId(), m.getGroupId(),
						m.getTimestamp(), m.getPreviousMessageId(),
						m.getSessionId(),
						getRandomBytes(MAX_PUBLIC_KEY_LENGTH),
						m.getAcceptTimestamp(), m.getTransportProperties())
		);
	}

	private void addTransportProperties()
			throws DbException, IOException, TimeoutException {
		TransportPropertyManager tpm0 = c0.getTransportPropertyManager();
		TransportPropertyManager tpm1 = c1.getTransportPropertyManager();
		TransportPropertyManager tpm2 = c2.getTransportPropertyManager();

		tpm0.mergeLocalProperties(TRANSPORT_ID, getTransportProperties(2));
		sync0To1(1, true);
		sync0To2(1, true);

		tpm1.mergeLocalProperties(TRANSPORT_ID, getTransportProperties(2));
		sync1To0(1, true);

		tpm2.mergeLocalProperties(TRANSPORT_ID, getTransportProperties(2));
		sync2To0(1, true);
	}

	private void assertDefaultUiMessages() throws DbException {
		Collection<IntroductionMessage> messages =
				introductionManager0.getIntroductionMessages(contactId1From0);
		assertEquals(2, messages.size());
		assertMessagesAreAcked(messages);

		messages = introductionManager0.getIntroductionMessages(
				contactId2From0);
		assertEquals(2, messages.size());
		assertMessagesAreAcked(messages);

		messages = introductionManager1.getIntroductionMessages(
				contactId0From1);
		assertEquals(2, messages.size());
		assertMessagesAreAcked(messages);

		messages = introductionManager2.getIntroductionMessages(
				contactId0From2);
		assertEquals(2, messages.size());
		assertMessagesAreAcked(messages);
	}

	private void assertMessagesAreAcked(
			Collection<IntroductionMessage> messages) {
		for (IntroductionMessage msg : messages) {
			if (msg.isLocal()) assertTrue(msg.isSeen());
		}
	}

	private void addListeners(boolean accept1, boolean accept2) {
		// listen to events
		listener0 = new IntroducerListener();
		c0.getEventBus().addListener(listener0);
		listener1 = new IntroduceeListener(1, accept1);
		c1.getEventBus().addListener(listener1);
		listener2 = new IntroduceeListener(2, accept2);
		c2.getEventBus().addListener(listener2);
	}

	@MethodsNotNullByDefault
	@ParametersNotNullByDefault
	private class IntroduceeListener implements EventListener {

		private volatile boolean requestReceived = false;
		private volatile boolean succeeded = false;
		private volatile boolean aborted = false;
		private volatile boolean answerRequests = true;
		private volatile SessionId sessionId;

		private final int introducee;
		private final boolean accept;

		private IntroduceeListener(int introducee, boolean accept) {
			this.introducee = introducee;
			this.accept = accept;
		}

		@Override
		public void eventOccurred(Event e) {
			if (e instanceof IntroductionRequestReceivedEvent) {
				IntroductionRequestReceivedEvent introEvent =
						((IntroductionRequestReceivedEvent) e);
				requestReceived = true;
				IntroductionRequest ir = introEvent.getIntroductionRequest();
				ContactId contactId = introEvent.getContactId();
				sessionId = ir.getSessionId();
				long time = clock.currentTimeMillis();
				try {
					if (introducee == 1 && answerRequests) {
						if (accept) {
							introductionManager1
									.acceptIntroduction(contactId, sessionId,
											time);
						} else {
							introductionManager1
									.declineIntroduction(contactId, sessionId,
											time);
						}
					} else if (introducee == 2 && answerRequests) {
						if (accept) {
							introductionManager2
									.acceptIntroduction(contactId, sessionId,
											time);
						} else {
							introductionManager2
									.declineIntroduction(contactId, sessionId,
											time);
						}
					}
				} catch (DbException exception) {
					eventWaiter.rethrow(exception);
				} finally {
					eventWaiter.resume();
				}
			} else if (e instanceof IntroductionSucceededEvent) {
				succeeded = true;
				Contact contact = ((IntroductionSucceededEvent) e).getContact();
				eventWaiter
						.assertFalse(contact.getId().equals(contactId0From1));
				eventWaiter.resume();
			} else if (e instanceof IntroductionAbortedEvent) {
				aborted = true;
				eventWaiter.resume();
			}
		}
	}

	@NotNullByDefault
	private class IntroducerListener implements EventListener {

		private volatile boolean response1Received = false;
		private volatile boolean response2Received = false;
		private volatile boolean aborted = false;

		@Override
		public void eventOccurred(Event e) {
			if (e instanceof IntroductionResponseReceivedEvent) {
				ContactId c =
						((IntroductionResponseReceivedEvent) e)
								.getContactId();
				if (c.equals(contactId1From0)) {
					response1Received = true;
				} else if (c.equals(contactId2From0)) {
					response2Received = true;
				}
				eventWaiter.resume();
			} else if (e instanceof IntroductionAbortedEvent) {
				aborted = true;
				eventWaiter.resume();
			}
		}

	}

	private void replacePreviousLocalMessageId(Author author,
			BdfDictionary d, MessageId id) throws FormatException {
		BdfDictionary i1 = d.getDictionary(SESSION_KEY_INTRODUCEE_1);
		BdfDictionary i2 = d.getDictionary(SESSION_KEY_INTRODUCEE_2);
		Author a1 = clientHelper
				.parseAndValidateAuthor(i1.getList(SESSION_KEY_AUTHOR));
		Author a2 = clientHelper
				.parseAndValidateAuthor(i2.getList(SESSION_KEY_AUTHOR));

		if (a1.equals(author)) {
			i1.put(SESSION_KEY_LAST_LOCAL_MESSAGE_ID, id);
			d.put(SESSION_KEY_INTRODUCEE_1, i1);
		} else if (a2.equals(author)) {
			i2.put(SESSION_KEY_LAST_LOCAL_MESSAGE_ID, id);
			d.put(SESSION_KEY_INTRODUCEE_2, i2);
		} else {
			throw new AssertionError();
		}
	}

	private AbstractIntroductionMessage getMessageFor(ClientHelper ch,
			Contact contact, MessageType type)
			throws FormatException, DbException {
		Group g = introductionManager0.getContactGroup(contact);
		BdfDictionary query = BdfDictionary.of(
				new BdfEntry(MSG_KEY_MESSAGE_TYPE, type.getValue())
		);
		Map<MessageId, BdfDictionary> map =
				ch.getMessageMetadataAsDictionary(g.getId(), query);
		assertEquals(1, map.size());
		MessageId id = map.entrySet().iterator().next().getKey();
		Message m = ch.getMessage(id);
		BdfList body = ch.getMessageAsList(id);
		//noinspection ConstantConditions
		return c0.getMessageParser().parseAcceptMessage(m, body);
	}

}

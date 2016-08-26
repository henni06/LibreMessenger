package org.briarproject;

import net.jodah.concurrentunit.Waiter;

import org.briarproject.api.clients.ClientHelper;
import org.briarproject.api.clients.SessionId;
import org.briarproject.api.contact.Contact;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.contact.ContactManager;
import org.briarproject.api.crypto.CryptoComponent;
import org.briarproject.api.crypto.KeyPair;
import org.briarproject.api.crypto.SecretKey;
import org.briarproject.api.data.BdfDictionary;
import org.briarproject.api.data.BdfEntry;
import org.briarproject.api.db.DatabaseComponent;
import org.briarproject.api.db.DbException;
import org.briarproject.api.db.Metadata;
import org.briarproject.api.db.Transaction;
import org.briarproject.api.event.Event;
import org.briarproject.api.event.EventListener;
import org.briarproject.api.event.IntroductionAbortedEvent;
import org.briarproject.api.event.IntroductionRequestReceivedEvent;
import org.briarproject.api.event.IntroductionResponseReceivedEvent;
import org.briarproject.api.event.IntroductionSucceededEvent;
import org.briarproject.api.event.MessageStateChangedEvent;
import org.briarproject.api.identity.AuthorFactory;
import org.briarproject.api.identity.IdentityManager;
import org.briarproject.api.identity.LocalAuthor;
import org.briarproject.api.introduction.IntroducerProtocolState;
import org.briarproject.api.introduction.IntroductionManager;
import org.briarproject.api.introduction.IntroductionMessage;
import org.briarproject.api.introduction.IntroductionRequest;
import org.briarproject.api.lifecycle.LifecycleManager;
import org.briarproject.api.properties.TransportProperties;
import org.briarproject.api.properties.TransportPropertyManager;
import org.briarproject.api.sync.ClientId;
import org.briarproject.api.sync.Group;
import org.briarproject.api.sync.MessageId;
import org.briarproject.api.sync.SyncSession;
import org.briarproject.api.sync.SyncSessionFactory;
import org.briarproject.api.sync.ValidationManager.State;
import org.briarproject.api.system.Clock;
import org.briarproject.contact.ContactModule;
import org.briarproject.crypto.CryptoModule;
import org.briarproject.introduction.IntroductionGroupFactory;
import org.briarproject.introduction.IntroductionModule;
import org.briarproject.introduction.MessageSender;
import org.briarproject.lifecycle.LifecycleModule;
import org.briarproject.properties.PropertiesModule;
import org.briarproject.sync.SyncModule;
import org.briarproject.system.SystemModule;
import org.briarproject.transport.TransportModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.inject.Inject;

import static org.briarproject.TestPluginsModule.MAX_LATENCY;
import static org.briarproject.TestPluginsModule.TRANSPORT_ID;
import static org.briarproject.api.identity.AuthorConstants.MAX_PUBLIC_KEY_LENGTH;
import static org.briarproject.api.identity.AuthorConstants.MAX_SIGNATURE_LENGTH;
import static org.briarproject.api.introduction.IntroductionConstants.ACCEPT;
import static org.briarproject.api.introduction.IntroductionConstants.E_PUBLIC_KEY;
import static org.briarproject.api.introduction.IntroductionConstants.GROUP_ID;
import static org.briarproject.api.introduction.IntroductionConstants.MAC;
import static org.briarproject.api.introduction.IntroductionConstants.MAC_LENGTH;
import static org.briarproject.api.introduction.IntroductionConstants.NAME;
import static org.briarproject.api.introduction.IntroductionConstants.PUBLIC_KEY;
import static org.briarproject.api.introduction.IntroductionConstants.SESSION_ID;
import static org.briarproject.api.introduction.IntroductionConstants.SIGNATURE;
import static org.briarproject.api.introduction.IntroductionConstants.STATE;
import static org.briarproject.api.introduction.IntroductionConstants.TIME;
import static org.briarproject.api.introduction.IntroductionConstants.TRANSPORT;
import static org.briarproject.api.introduction.IntroductionConstants.TYPE;
import static org.briarproject.api.introduction.IntroductionConstants.TYPE_ACK;
import static org.briarproject.api.introduction.IntroductionConstants.TYPE_REQUEST;
import static org.briarproject.api.introduction.IntroductionConstants.TYPE_RESPONSE;
import static org.briarproject.api.sync.ValidationManager.State.DELIVERED;
import static org.briarproject.api.sync.ValidationManager.State.INVALID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IntroductionIntegrationTest extends BriarTestCase {

	private LifecycleManager lifecycleManager0, lifecycleManager1, lifecycleManager2;
	private SyncSessionFactory sync0, sync1, sync2;
	private ContactManager contactManager0, contactManager1, contactManager2;
	private ContactId contactId0, contactId1, contactId2;
	private IdentityManager identityManager0, identityManager1, identityManager2;
	private LocalAuthor author0, author1, author2;

	@Inject
	Clock clock;
	@Inject
	CryptoComponent crypto;
	@Inject
	AuthorFactory authorFactory;

	// objects accessed from background threads need to be volatile
	private volatile IntroductionManager introductionManager0;
	private volatile IntroductionManager introductionManager1;
	private volatile IntroductionManager introductionManager2;
	private volatile Waiter eventWaiter;
	private volatile Waiter msgWaiter;

	private final File testDir = TestUtils.getTestDirectory();
	private final SecretKey master = TestUtils.getSecretKey();
	private final int TIMEOUT = 15000;
	private final String INTRODUCER = "Introducer";
	private final String INTRODUCEE1 = "Introducee1";
	private final String INTRODUCEE2 = "Introducee2";

	private static final Logger LOG =
			Logger.getLogger(IntroductionIntegrationTest.class.getName());

	private IntroductionIntegrationTestComponent t0, t1, t2;

	@Before
	public void setUp() {
		IntroductionIntegrationTestComponent component =
				DaggerIntroductionIntegrationTestComponent.builder().build();
		component.inject(this);
		injectEagerSingletons(component);

		assertTrue(testDir.mkdirs());
		File t0Dir = new File(testDir, INTRODUCER);
		t0 = DaggerIntroductionIntegrationTestComponent.builder()
				.testDatabaseModule(new TestDatabaseModule(t0Dir)).build();
		injectEagerSingletons(t0);
		File t1Dir = new File(testDir, INTRODUCEE1);
		t1 = DaggerIntroductionIntegrationTestComponent.builder()
				.testDatabaseModule(new TestDatabaseModule(t1Dir)).build();
		injectEagerSingletons(t1);
		File t2Dir = new File(testDir, INTRODUCEE2);
		t2 = DaggerIntroductionIntegrationTestComponent.builder()
				.testDatabaseModule(new TestDatabaseModule(t2Dir)).build();
		injectEagerSingletons(t2);

		identityManager0 = t0.getIdentityManager();
		identityManager1 = t1.getIdentityManager();
		identityManager2 = t2.getIdentityManager();
		contactManager0 = t0.getContactManager();
		contactManager1 = t1.getContactManager();
		contactManager2 = t2.getContactManager();
		introductionManager0 = t0.getIntroductionManager();
		introductionManager1 = t1.getIntroductionManager();
		introductionManager2 = t2.getIntroductionManager();
		sync0 = t0.getSyncSessionFactory();
		sync1 = t1.getSyncSessionFactory();
		sync2 = t2.getSyncSessionFactory();

		// initialize waiters fresh for each test
		eventWaiter = new Waiter();
		msgWaiter = new Waiter();
	}

	@Test
	public void testIntroductionSession() throws Exception {
		startLifecycles();
		try {
			// Add Identities And Contacts
			addDefaultIdentities();
			addDefaultContacts();

			// Add Transport Properties
			addTransportProperties();

			// listen to events
			IntroducerListener listener0 = new IntroducerListener();
			t0.getEventBus().addListener(listener0);
			IntroduceeListener listener1 = new IntroduceeListener(1, true);
			t1.getEventBus().addListener(listener1);
			IntroduceeListener listener2 = new IntroduceeListener(2, true);
			t2.getEventBus().addListener(listener2);

			// make introduction
			long time = clock.currentTimeMillis();
			Contact introducee1 = contactManager0.getContact(contactId1);
			Contact introducee2 = contactManager0.getContact(contactId2);
			introductionManager0
					.makeIntroduction(introducee1, introducee2, "Hi!", time);

			// sync first request message
			deliverMessage(sync0, contactId0, sync1, contactId1, "0 to 1");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener1.requestReceived);

			// sync second request message
			deliverMessage(sync0, contactId0, sync2, contactId2, "0 to 2");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener2.requestReceived);

			// sync first response
			deliverMessage(sync1, contactId1, sync0, contactId0, "1 to 0");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener0.response1Received);

			// sync second response
			deliverMessage(sync2, contactId2, sync0, contactId0, "2 to 0");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener0.response2Received);

			// sync forwarded responses to introducees
			deliverMessage(sync0, contactId0, sync1, contactId1, "0 to 1");
			deliverMessage(sync0, contactId0, sync2, contactId2, "0 to 2");

			// sync first ACK and its forward
			deliverMessage(sync1, contactId1, sync0, contactId0, "1 to 0");
			deliverMessage(sync0, contactId0, sync2, contactId2, "0 to 2");

			// sync second ACK and its forward
			deliverMessage(sync2, contactId2, sync0, contactId0, "2 to 0");
			deliverMessage(sync0, contactId0, sync1, contactId1, "0 to 2");

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
		} finally {
			stopLifecycles();
		}
	}

	@Test
	public void testIntroductionSessionFirstDecline() throws Exception {
		startLifecycles();
		try {
			// Add Identities And Contacts
			addDefaultIdentities();
			addDefaultContacts();

			// Add Transport Properties
			addTransportProperties();

			// listen to events
			IntroducerListener listener0 = new IntroducerListener();
			t0.getEventBus().addListener(listener0);
			IntroduceeListener listener1 = new IntroduceeListener(1, false);
			t1.getEventBus().addListener(listener1);
			IntroduceeListener listener2 = new IntroduceeListener(2, true);
			t2.getEventBus().addListener(listener2);

			// make introduction
			long time = clock.currentTimeMillis();
			Contact introducee1 = contactManager0.getContact(contactId1);
			Contact introducee2 = contactManager0.getContact(contactId2);
			introductionManager0
					.makeIntroduction(introducee1, introducee2, null, time);

			// sync request messages
			deliverMessage(sync0, contactId0, sync1, contactId1);
			deliverMessage(sync0, contactId0, sync2, contactId2);

			// wait for requests to arrive
			eventWaiter.await(TIMEOUT, 2);
			assertTrue(listener1.requestReceived);
			assertTrue(listener2.requestReceived);

			// sync first response
			deliverMessage(sync1, contactId1, sync0, contactId0, "1 to 0");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener0.response1Received);

			// sync second response
			deliverMessage(sync2, contactId2, sync0, contactId0, "2 to 0");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener0.response2Received);

			// sync first forwarded response
			deliverMessage(sync0, contactId0, sync2, contactId2);

			// note how the introducer does not forward the second response,
			// because after the first decline the protocol finished

			assertFalse(listener1.succeeded);
			assertFalse(listener2.succeeded);

			assertFalse(contactManager1
					.contactExists(author2.getId(), author1.getId()));
			assertFalse(contactManager2
					.contactExists(author1.getId(), author2.getId()));

			assertEquals(2,
					introductionManager0.getIntroductionMessages(contactId1)
							.size());
			assertEquals(2,
					introductionManager0.getIntroductionMessages(contactId2)
							.size());
			assertEquals(2,
					introductionManager1.getIntroductionMessages(contactId0)
							.size());
			// introducee2 should also have the decline response of introducee1
			assertEquals(3,
					introductionManager2.getIntroductionMessages(contactId0)
							.size());
		} finally {
			stopLifecycles();
		}
	}

	@Test
	public void testIntroductionSessionSecondDecline() throws Exception {
		startLifecycles();
		try {
			// Add Identities And Contacts
			addDefaultIdentities();
			addDefaultContacts();

			// Add Transport Properties
			addTransportProperties();

			// listen to events
			IntroducerListener listener0 = new IntroducerListener();
			t0.getEventBus().addListener(listener0);
			IntroduceeListener listener1 = new IntroduceeListener(1, true);
			t1.getEventBus().addListener(listener1);
			IntroduceeListener listener2 = new IntroduceeListener(2, false);
			t2.getEventBus().addListener(listener2);

			// make introduction
			long time = clock.currentTimeMillis();
			Contact introducee1 = contactManager0.getContact(contactId1);
			Contact introducee2 = contactManager0.getContact(contactId2);
			introductionManager0
					.makeIntroduction(introducee1, introducee2, null, time);

			// sync request messages
			deliverMessage(sync0, contactId0, sync1, contactId1);
			deliverMessage(sync0, contactId0, sync2, contactId2);

			// wait for requests to arrive
			eventWaiter.await(TIMEOUT, 2);
			assertTrue(listener1.requestReceived);
			assertTrue(listener2.requestReceived);

			// sync first response
			deliverMessage(sync1, contactId1, sync0, contactId0, "1 to 0");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener0.response1Received);

			// sync second response
			deliverMessage(sync2, contactId2, sync0, contactId0, "2 to 0");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener0.response2Received);

			// sync both forwarded response
			deliverMessage(sync0, contactId0, sync2, contactId2);
			deliverMessage(sync0, contactId0, sync1, contactId1);

			assertFalse(contactManager1
					.contactExists(author2.getId(), author1.getId()));
			assertFalse(contactManager2
					.contactExists(author1.getId(), author2.getId()));

			assertEquals(2,
					introductionManager0.getIntroductionMessages(contactId1)
							.size());
			assertEquals(2,
					introductionManager0.getIntroductionMessages(contactId2)
							.size());
			// introducee1 also sees the decline response from introducee2
			assertEquals(3,
					introductionManager1.getIntroductionMessages(contactId0)
							.size());
			assertEquals(2,
					introductionManager2.getIntroductionMessages(contactId0)
							.size());
		} finally {
			stopLifecycles();
		}
	}

	@Test
	public void testIntroductionSessionDelayedFirstDecline() throws Exception {
		startLifecycles();
		try {
			// Add Identities And Contacts
			addDefaultIdentities();
			addDefaultContacts();

			// Add Transport Properties
			addTransportProperties();

			// listen to events
			IntroducerListener listener0 = new IntroducerListener();
			t0.getEventBus().addListener(listener0);
			IntroduceeListener listener1 = new IntroduceeListener(1, false);
			t1.getEventBus().addListener(listener1);
			IntroduceeListener listener2 = new IntroduceeListener(2, false);
			t2.getEventBus().addListener(listener2);

			// make introduction
			long time = clock.currentTimeMillis();
			Contact introducee1 = contactManager0.getContact(contactId1);
			Contact introducee2 = contactManager0.getContact(contactId2);
			introductionManager0
					.makeIntroduction(introducee1, introducee2, null, time);

			// sync request messages
			deliverMessage(sync0, contactId0, sync1, contactId1);
			deliverMessage(sync0, contactId0, sync2, contactId2);

			// wait for requests to arrive
			eventWaiter.await(TIMEOUT, 2);
			assertTrue(listener1.requestReceived);
			assertTrue(listener2.requestReceived);

			// sync first response
			deliverMessage(sync1, contactId1, sync0, contactId0, "1 to 0");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener0.response1Received);

			// sync second response
			deliverMessage(sync2, contactId2, sync0, contactId0, "2 to 0");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener0.response2Received);

			// sync first forwarded response
			deliverMessage(sync0, contactId0, sync2, contactId2);

			// note how the second response will not be forwarded anymore

			assertFalse(contactManager1
					.contactExists(author2.getId(), author1.getId()));
			assertFalse(contactManager2
					.contactExists(author1.getId(), author2.getId()));

			// since introducee2 was already in FINISHED state when
			// introducee1's response arrived, she ignores and deletes it
			assertDefaultUiMessages();
		} finally {
			stopLifecycles();
		}
	}

	@Test
	public void testIntroductionToSameContact() throws Exception {
		startLifecycles();
		try {
			// Add Identities And Contacts
			addDefaultIdentities();
			addDefaultContacts();

			// Add Transport Properties
			addTransportProperties();

			// listen to events
			IntroducerListener listener0 = new IntroducerListener();
			t0.getEventBus().addListener(listener0);
			IntroduceeListener listener1 = new IntroduceeListener(1, true);
			t1.getEventBus().addListener(listener1);

			// make introduction
			long time = clock.currentTimeMillis();
			Contact introducee1 = contactManager0.getContact(contactId1);
			introductionManager0
					.makeIntroduction(introducee1, introducee1, null, time);

			// sync request messages
			deliverMessage(sync0, contactId0, sync1, contactId1);

			// we should not get any event, because the request will be discarded
			assertFalse(listener1.requestReceived);

			// make really sure we don't have that request
			assertTrue(introductionManager1.getIntroductionMessages(contactId0)
					.isEmpty());
		} finally {
			stopLifecycles();
		}
	}

	@Test
	public void testIntroductionToIdentitiesOfSameContact() throws Exception {
		startLifecycles();
		try {
			// Add Identities
			author0 = authorFactory.createLocalAuthor(INTRODUCER,
					TestUtils.getRandomBytes(MAX_PUBLIC_KEY_LENGTH),
					TestUtils.getRandomBytes(123));
			identityManager0.addLocalAuthor(author0);
			author1 = authorFactory.createLocalAuthor(INTRODUCEE1,
					TestUtils.getRandomBytes(MAX_PUBLIC_KEY_LENGTH),
					TestUtils.getRandomBytes(123));
			identityManager1.addLocalAuthor(author1);
			author2 = authorFactory.createLocalAuthor(INTRODUCEE2,
					TestUtils.getRandomBytes(MAX_PUBLIC_KEY_LENGTH),
					TestUtils.getRandomBytes(123));
			identityManager1.addLocalAuthor(author2);

			// Add Transport Properties
			addTransportProperties();

			// Add introducees' authors as contacts
			contactId1 = contactManager0.addContact(author1,
					author0.getId(), master, clock.currentTimeMillis(), true,
					true, true
			);
			contactId2 = contactManager0.addContact(author2,
					author0.getId(), master, clock.currentTimeMillis(), true,
					true, true
			);
			// Add introducer back
			contactId0 = null;
			ContactId contactId01 = contactManager1.addContact(author0,
					author1.getId(), master, clock.currentTimeMillis(), false,
					true, true
			);
			ContactId contactId02 = contactManager1.addContact(author0,
					author2.getId(), master, clock.currentTimeMillis(), false,
					true, true
			);

			// listen to events
			IntroducerListener listener0 = new IntroducerListener();
			t0.getEventBus().addListener(listener0);
			IntroduceeListener listener1 = new IntroduceeListener(1, true);
			t1.getEventBus().addListener(listener1);

			// make introduction
			long time = clock.currentTimeMillis();
			Contact introducee1 = contactManager0.getContact(contactId1);
			Contact introducee2 = contactManager0.getContact(contactId2);
			introductionManager0
					.makeIntroduction(introducee1, introducee2, "Hi!", time);

			// sync request messages
			deliverMessage(sync0, contactId01, sync1, contactId1);
			deliverMessage(sync0, contactId02, sync1, contactId2);

			// wait for request to arrive
			eventWaiter.await(TIMEOUT, 2);
			assertTrue(listener1.requestReceived);

			// sync responses
			deliverMessage(sync1, contactId1, sync0, contactId01);
			deliverMessage(sync1, contactId2, sync0, contactId02);

			// wait for two responses to arrive
			eventWaiter.await(TIMEOUT, 2);
			assertTrue(listener0.response1Received);
			assertTrue(listener0.response2Received);

			// sync forwarded responses to introducees
			deliverMessage(sync0, contactId01, sync1, contactId1);
			deliverMessage(sync0, contactId02, sync1, contactId2);

			// wait for "both" introducees to abort session
			eventWaiter.await(TIMEOUT, 2);
			assertTrue(listener1.aborted);

			// sync abort message
			deliverMessage(sync1, contactId1, sync0, contactId01);
			deliverMessage(sync1, contactId2, sync0, contactId02);

			// wait for introducer to abort session (gets event twice)
			eventWaiter.await(TIMEOUT, 2);
			assertTrue(listener0.aborted);

			assertFalse(contactManager1
					.contactExists(author1.getId(), author2.getId()));
			assertFalse(contactManager1
					.contactExists(author2.getId(), author1.getId()));

			assertEquals(2, introductionManager0.getIntroductionMessages(
					contactId1).size());
			assertEquals(2, introductionManager0.getIntroductionMessages(
					contactId2).size());
			assertEquals(2, introductionManager1.getIntroductionMessages(
					contactId01).size());
			assertEquals(2, introductionManager1.getIntroductionMessages(
					contactId02).size());
		} finally {
			stopLifecycles();
		}
	}

	@Test
	public void testSessionIdReuse() throws Exception {
		startLifecycles();
		try {
			// Add Identities And Contacts
			addDefaultIdentities();
			addDefaultContacts();

			// Add Transport Properties
			addTransportProperties();

			// listen to events
			IntroducerListener listener0 = new IntroducerListener();
			t0.getEventBus().addListener(listener0);
			IntroduceeListener listener1 = new IntroduceeListener(1, true);
			t1.getEventBus().addListener(listener1);
			IntroduceeListener listener2 = new IntroduceeListener(2, true);
			t2.getEventBus().addListener(listener2);

			// make introduction
			long time = clock.currentTimeMillis();
			Contact introducee1 = contactManager0.getContact(contactId1);
			Contact introducee2 = contactManager0.getContact(contactId2);
			introductionManager0
					.makeIntroduction(introducee1, introducee2, "Hi!", time);

			// sync first request message
			deliverMessage(sync0, contactId0, sync1, contactId1, "0 to 1");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener1.requestReceived);

			// get SessionId
			List<IntroductionMessage> list = new ArrayList<>(
					introductionManager1.getIntroductionMessages(contactId0));
			assertEquals(2, list.size());
			assertTrue(list.get(0) instanceof IntroductionRequest);
			IntroductionRequest msg = (IntroductionRequest) list.get(0);
			SessionId sessionId = msg.getSessionId();

			// get contact group
			IntroductionGroupFactory groupFactory =
					t0.getIntroductionGroupFactory();
			Group group = groupFactory.createIntroductionGroup(introducee1);

			// create new message with same SessionId
			BdfDictionary d = BdfDictionary.of(
					new BdfEntry(TYPE, TYPE_REQUEST),
					new BdfEntry(SESSION_ID, sessionId),
					new BdfEntry(GROUP_ID, group.getId()),
					new BdfEntry(NAME, TestUtils.getRandomString(42)),
					new BdfEntry(PUBLIC_KEY,
							TestUtils.getRandomBytes(MAX_PUBLIC_KEY_LENGTH))
			);

			// reset request received state
			listener1.requestReceived = false;

			// add the message to the queue
			DatabaseComponent db0 = t0.getDatabaseComponent();
			MessageSender sender0 = t0.getMessageSender();
			Transaction txn = db0.startTransaction(false);
			try {
				sender0.sendMessage(txn, d);
				txn.setComplete();
			} finally {
				db0.endTransaction(txn);
			}

			// actually send message
			deliverMessage(sync0, contactId0, sync1, contactId1, "0 to 1");

			// make sure it does not arrive
			assertFalse(listener1.requestReceived);
		} finally {
			stopLifecycles();
		}
	}

	@Test
	public void testIntroducerRemovedCleanup() throws Exception {
		startLifecycles();
		try {
			// Add Identities And Contacts
			addDefaultIdentities();
			addDefaultContacts();

			// Add Transport Properties
			addTransportProperties();

			// listen to events
			IntroducerListener listener0 = new IntroducerListener();
			t0.getEventBus().addListener(listener0);
			IntroduceeListener listener1 = new IntroduceeListener(1, true);
			t1.getEventBus().addListener(listener1);
			IntroduceeListener listener2 = new IntroduceeListener(2, true);
			t2.getEventBus().addListener(listener2);

			// make introduction
			long time = clock.currentTimeMillis();
			Contact introducee1 = contactManager0.getContact(contactId1);
			Contact introducee2 = contactManager0.getContact(contactId2);
			introductionManager0
					.makeIntroduction(introducee1, introducee2, "Hi!", time);

			// sync first request message
			deliverMessage(sync0, contactId0, sync1, contactId1, "0 to 1");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener1.requestReceived);

			// get database and local group for introducee
			DatabaseComponent db1 = t1.getDatabaseComponent();
			IntroductionGroupFactory groupFactory1 =
					t1.getIntroductionGroupFactory();
			Group group1 = groupFactory1.createLocalGroup();

			// get local session state messages
			Map<MessageId, Metadata> map;
			Transaction txn = db1.startTransaction(false);
			try {
				map = db1.getMessageMetadata(txn, group1.getId());
				txn.setComplete();
			} finally {
				db1.endTransaction(txn);
			}
			// check that we have one session state
			assertEquals(1, map.size());

			// introducee1 removes introducer
			contactManager1.removeContact(contactId0);

			// get local session state messages again
			txn = db1.startTransaction(false);
			try {
				map = db1.getMessageMetadata(txn, group1.getId());
				txn.setComplete();
			} finally {
				db1.endTransaction(txn);
			}
			// make sure local state got deleted
			assertEquals(0, map.size());
		} finally {
			stopLifecycles();
		}
	}

	@Test
	public void testIntroduceesRemovedCleanup() throws Exception {
		startLifecycles();
		try {
			// Add Identities And Contacts
			addDefaultIdentities();
			addDefaultContacts();

			// Add Transport Properties
			addTransportProperties();

			// listen to events
			IntroducerListener listener0 = new IntroducerListener();
			t0.getEventBus().addListener(listener0);
			IntroduceeListener listener1 = new IntroduceeListener(1, true);
			t1.getEventBus().addListener(listener1);
			IntroduceeListener listener2 = new IntroduceeListener(2, true);
			t2.getEventBus().addListener(listener2);

			// make introduction
			long time = clock.currentTimeMillis();
			Contact introducee1 = contactManager0.getContact(contactId1);
			Contact introducee2 = contactManager0.getContact(contactId2);
			introductionManager0
					.makeIntroduction(introducee1, introducee2, "Hi!", time);

			// sync first request message
			deliverMessage(sync0, contactId0, sync1, contactId1, "0 to 1");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener1.requestReceived);

			// get database and local group for introducee
			DatabaseComponent db0 = t0.getDatabaseComponent();
			IntroductionGroupFactory groupFactory0 =
					t0.getIntroductionGroupFactory();
			Group group1 = groupFactory0.createLocalGroup();

			// get local session state messages
			Map<MessageId, Metadata> map;
			Transaction txn = db0.startTransaction(false);
			try {
				map = db0.getMessageMetadata(txn, group1.getId());
				txn.setComplete();
			} finally {
				db0.endTransaction(txn);
			}
			// check that we have one session state
			assertEquals(1, map.size());

			// introducer removes introducee1
			contactManager0.removeContact(contactId1);

			// get local session state messages again
			txn = db0.startTransaction(false);
			try {
				map = db0.getMessageMetadata(txn, group1.getId());
				txn.setComplete();
			} finally {
				db0.endTransaction(txn);
			}
			// make sure local state is still there
			assertEquals(1, map.size());

			// introducer removes other introducee
			contactManager0.removeContact(contactId2);

			// get local session state messages again
			txn = db0.startTransaction(false);
			try {
				map = db0.getMessageMetadata(txn, group1.getId());
				txn.setComplete();
			} finally {
				db0.endTransaction(txn);
			}
			// make sure local state is gone now
			assertEquals(0, map.size());
		} finally {
			stopLifecycles();
		}
	}

	@Test
	public void testFakeResponse() throws Exception {
		startLifecycles();
		try {
			addDefaultIdentities();
			addDefaultContacts();
			addTransportProperties();

			// listen to events
			IntroducerListener listener0 = new IntroducerListener();
			t0.getEventBus().addListener(listener0);
			IntroduceeListener listener1 = new IntroduceeListener(1, true);
			t1.getEventBus().addListener(listener1);
			IntroduceeListener listener2 = new IntroduceeListener(2, true);
			t2.getEventBus().addListener(listener2);

			// make introduction
			long time = clock.currentTimeMillis();
			Contact introducee1 = contactManager0.getContact(contactId1);
			Contact introducee2 = contactManager0.getContact(contactId2);
			introductionManager0
					.makeIntroduction(introducee1, introducee2, "Hi!", time);

			// sync first request message
			deliverMessage(sync0, contactId0, sync1, contactId1, "0 to 1");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener1.requestReceived);

			// sync first response
			deliverMessage(sync1, contactId1, sync0, contactId0, "1 to 0");
			eventWaiter.await(TIMEOUT, 1);
			assertTrue(listener0.response1Received);

			// get SessionId
			List<IntroductionMessage> list = new ArrayList<>(
					introductionManager1.getIntroductionMessages(contactId0));
			assertEquals(2, list.size());
			assertTrue(list.get(0) instanceof IntroductionRequest);
			IntroductionRequest msg = (IntroductionRequest) list.get(0);
			SessionId sessionId = msg.getSessionId();

			// get contact group
			IntroductionGroupFactory groupFactory =
					t0.getIntroductionGroupFactory();
			Group group = groupFactory.createIntroductionGroup(introducee1);

			// get data for contact2
			long timestamp = clock.currentTimeMillis();
			KeyPair eKeyPair = crypto.generateAgreementKeyPair();
			byte[] ePublicKey = eKeyPair.getPublic().getEncoded();
			TransportProperties tp = new TransportProperties(
					Collections.singletonMap("key", "value"));
			BdfDictionary tpDict = BdfDictionary.of(new BdfEntry("fake", tp));

			// create a fake response
			BdfDictionary d = BdfDictionary.of(
					new BdfEntry(TYPE, TYPE_RESPONSE),
					new BdfEntry(SESSION_ID, sessionId),
					new BdfEntry(GROUP_ID, group.getId()),
					new BdfEntry(ACCEPT, true),
					new BdfEntry(TIME, timestamp),
					new BdfEntry(E_PUBLIC_KEY, ePublicKey),
					new BdfEntry(TRANSPORT, tpDict)
			);

			// add the message to the queue
			DatabaseComponent db0 = t0.getDatabaseComponent();
			MessageSender sender0 = t0.getMessageSender();
			Transaction txn = db0.startTransaction(false);
			try {
				sender0.sendMessage(txn, d);
				txn.setComplete();
			} finally {
				db0.endTransaction(txn);
			}

			// send the fake response
			deliverMessage(sync0, contactId0, sync1, contactId1, "0 to 1");

			// fake session state for introducer, so she doesn't abort
			ClientHelper clientHelper0 = t0.getClientHelper();
			BdfDictionary state =
					clientHelper0.getMessageMetadataAsDictionary(sessionId);
			state.put(STATE, IntroducerProtocolState.AWAIT_ACKS.getValue());
			clientHelper0.mergeMessageMetadata(sessionId, state);

			// sync back the ACK
			deliverMessage(sync1, contactId1, sync0, contactId0, "1 to 0");

			// create a fake ACK
			// TODO do we need to actually calculate a MAC and signature here?
			byte[] mac = TestUtils.getRandomBytes(MAC_LENGTH);
			byte[] sig = TestUtils.getRandomBytes(MAX_SIGNATURE_LENGTH);
			d = BdfDictionary.of(
					new BdfEntry(TYPE, TYPE_ACK),
					new BdfEntry(SESSION_ID, sessionId),
					new BdfEntry(GROUP_ID, group.getId()),
					new BdfEntry(MAC, mac),
					new BdfEntry(SIGNATURE, sig)
			);

			// add the fake ACK to the message queue
			txn = db0.startTransaction(false);
			try {
				sender0.sendMessage(txn, d);
				txn.setComplete();
			} finally {
				db0.endTransaction(txn);
			}

			// make sure the contact was already added (as inactive)
			DatabaseComponent db1 = t1.getDatabaseComponent();
			txn = db1.startTransaction(true);
			try {
				assertEquals(2, db1.getContacts(txn).size());
				txn.setComplete();
			} finally {
				db1.endTransaction(txn);
			}

			// send the fake ACK
			deliverMessage(sync0, contactId0, sync1, contactId1, "0 to 1");

			// make sure session was aborted and contact deleted again
			txn = db1.startTransaction(true);
			try {
				assertEquals(1, db1.getContacts(txn).size());
				txn.setComplete();
			} finally {
				db1.endTransaction(txn);
			}

			// there should now be an abort message to sync back
			deliverMessage(sync1, contactId1, sync0, contactId0, "1 to 0");

			// ensure introducer got the abort
			state = clientHelper0.getMessageMetadataAsDictionary(sessionId);
			assertEquals(IntroducerProtocolState.ERROR.getValue(),
					state.getLong(STATE).intValue());
		} finally {
			stopLifecycles();
		}
	}

	@After
	public void tearDown() throws InterruptedException {
		TestUtils.deleteTestDirectory(testDir);
	}

	private void startLifecycles() throws InterruptedException {
		// Start the lifecycle manager and wait for it to finish
		lifecycleManager0 = t0.getLifecycleManager();
		lifecycleManager1 = t1.getLifecycleManager();
		lifecycleManager2 = t2.getLifecycleManager();
		lifecycleManager0.startServices();
		lifecycleManager1.startServices();
		lifecycleManager2.startServices();
		lifecycleManager0.waitForStartup();
		lifecycleManager1.waitForStartup();
		lifecycleManager2.waitForStartup();
	}

	private void stopLifecycles() throws InterruptedException {
		// Clean up
		lifecycleManager0.stopServices();
		lifecycleManager1.stopServices();
		lifecycleManager2.stopServices();
		lifecycleManager0.waitForShutdown();
		lifecycleManager1.waitForShutdown();
		lifecycleManager2.waitForShutdown();
	}

	private void addTransportProperties() throws DbException {
		TransportPropertyManager tpm0 = t0.getTransportPropertyManager();
		TransportPropertyManager tpm1 = t1.getTransportPropertyManager();
		TransportPropertyManager tpm2 = t2.getTransportPropertyManager();

		TransportProperties tp = new TransportProperties(
				Collections.singletonMap("key", "value"));
		tpm0.mergeLocalProperties(TRANSPORT_ID, tp);
		tpm1.mergeLocalProperties(TRANSPORT_ID, tp);
		tpm2.mergeLocalProperties(TRANSPORT_ID, tp);
	}

	private void addDefaultIdentities() throws DbException {
		KeyPair keyPair0 = crypto.generateSignatureKeyPair();
		byte[] publicKey0 = keyPair0.getPublic().getEncoded();
		byte[] privateKey0 = keyPair0.getPrivate().getEncoded();
		author0 = authorFactory
				.createLocalAuthor(INTRODUCER, publicKey0, privateKey0);
		identityManager0.addLocalAuthor(author0);
		KeyPair keyPair1 = crypto.generateSignatureKeyPair();
		byte[] publicKey1 = keyPair1.getPublic().getEncoded();
		byte[] privateKey1 = keyPair1.getPrivate().getEncoded();
		author1 = authorFactory
				.createLocalAuthor(INTRODUCEE1, publicKey1, privateKey1);
		identityManager1.addLocalAuthor(author1);
		KeyPair keyPair2 = crypto.generateSignatureKeyPair();
		byte[] publicKey2 = keyPair2.getPublic().getEncoded();
		byte[] privateKey2 = keyPair2.getPrivate().getEncoded();
		author2 = authorFactory
				.createLocalAuthor(INTRODUCEE2, publicKey2, privateKey2);
		identityManager2.addLocalAuthor(author2);
	}

	private void addDefaultContacts() throws DbException {
		// Add introducees as contacts
		contactId1 = contactManager0.addContact(author1,
				author0.getId(), master, clock.currentTimeMillis(), true,
				true, true
		);
		contactId2 = contactManager0.addContact(author2,
				author0.getId(), master, clock.currentTimeMillis(), true,
				true, true
		);
		// Add introducer back
		contactId0 = contactManager1.addContact(author0,
				author1.getId(), master, clock.currentTimeMillis(), true,
				true, true
		);
		ContactId contactId02 = contactManager2.addContact(author0,
				author2.getId(), master, clock.currentTimeMillis(), true,
				true, true
		);
		assertTrue(contactId0.equals(contactId02));
	}

	private void deliverMessage(SyncSessionFactory fromSync, ContactId fromId,
			SyncSessionFactory toSync, ContactId toId)
			throws IOException, TimeoutException {
		deliverMessage(fromSync, fromId, toSync, toId, null);
	}

	private void deliverMessage(SyncSessionFactory fromSync, ContactId fromId,
			SyncSessionFactory toSync, ContactId toId, String debug)
			throws IOException, TimeoutException {

		if (debug != null) LOG.info("TEST: Sending message from " + debug);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// Create an outgoing sync session
		SyncSession sessionFrom =
				fromSync.createSimplexOutgoingSession(toId, MAX_LATENCY, out);
		// Write whatever needs to be written
		sessionFrom.run();
		out.close();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		// Create an incoming sync session
		SyncSession sessionTo = toSync.createIncomingSession(fromId, in);
		// Read whatever needs to be read
		sessionTo.run();
		in.close();

		// wait for message to actually arrive
		msgWaiter.await(TIMEOUT, 1);
	}

	private void assertDefaultUiMessages() throws DbException {
		assertEquals(2, introductionManager0.getIntroductionMessages(
				contactId1).size());
		assertEquals(2, introductionManager0.getIntroductionMessages(
				contactId2).size());
		assertEquals(2, introductionManager1.getIntroductionMessages(
				contactId0).size());
		assertEquals(2, introductionManager2.getIntroductionMessages(
				contactId0).size());
	}

	private class IntroduceeListener implements EventListener {

		private volatile boolean requestReceived = false;
		private volatile boolean succeeded = false;
		private volatile boolean aborted = false;

		private final int introducee;
		private final boolean accept;

		private IntroduceeListener(int introducee, boolean accept) {
			this.introducee = introducee;
			this.accept = accept;
		}

		@Override
		public void eventOccurred(Event e) {
			if (e instanceof MessageStateChangedEvent) {
				MessageStateChangedEvent event = (MessageStateChangedEvent) e;
				State s = event.getState();
				ClientId c = event.getClientId();
				if ((s == DELIVERED || s == INVALID) &&
						c.equals(introductionManager0.getClientId()) &&
						!event.isLocal()) {
					LOG.info("TEST: Introducee" + introducee +
							" received message in group " +
							event.getMessage().getGroupId().hashCode());
					msgWaiter.resume();
				}
			} else if (e instanceof IntroductionRequestReceivedEvent) {
				IntroductionRequestReceivedEvent introEvent =
						((IntroductionRequestReceivedEvent) e);
				requestReceived = true;
				IntroductionRequest ir = introEvent.getIntroductionRequest();
				ContactId contactId = introEvent.getContactId();
				SessionId sessionId = ir.getSessionId();
				long time = clock.currentTimeMillis();
				try {
					if (introducee == 1) {
						if (accept) {
							introductionManager1
									.acceptIntroduction(contactId, sessionId,
											time);
						} else {
							introductionManager1
									.declineIntroduction(contactId, sessionId,
											time);
						}
					} else if (introducee == 2) {
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
				} catch (DbException | IOException exception) {
					eventWaiter.rethrow(exception);
				} finally {
					eventWaiter.resume();
				}
			} else if (e instanceof IntroductionSucceededEvent) {
				succeeded = true;
				Contact contact = ((IntroductionSucceededEvent) e).getContact();
				eventWaiter.assertFalse(contact.getId().equals(contactId0));
				eventWaiter.assertTrue(contact.isActive());
				eventWaiter.resume();
			} else if (e instanceof IntroductionAbortedEvent) {
				aborted = true;
				eventWaiter.resume();
			}
		}
	}

	private class IntroducerListener implements EventListener {

		private volatile boolean response1Received = false;
		private volatile boolean response2Received = false;
		private volatile boolean aborted = false;

		@Override
		public void eventOccurred(Event e) {
			if (e instanceof MessageStateChangedEvent) {
				MessageStateChangedEvent event = (MessageStateChangedEvent) e;
				if (event.getState() == DELIVERED && event.getClientId()
						.equals(introductionManager0.getClientId()) &&
						!event.isLocal()) {
					LOG.info("TEST: Introducer received message in group " +
							event.getMessage().getGroupId().hashCode());
					msgWaiter.resume();
				}
			} else if (e instanceof IntroductionResponseReceivedEvent) {
				ContactId c =
						((IntroductionResponseReceivedEvent) e).getContactId();
				try {
					if (c.equals(contactId1)) {
						response1Received = true;
					} else if (c.equals(contactId2)) {
						response2Received = true;
					}
				} finally {
					eventWaiter.resume();
				}
			} else if (e instanceof IntroductionAbortedEvent) {
				aborted = true;
				eventWaiter.resume();
			}
		}
	}

	private void injectEagerSingletons(
			IntroductionIntegrationTestComponent component) {

		component.inject(new LifecycleModule.EagerSingletons());
		component.inject(new LifecycleModule.EagerSingletons());
		component.inject(new IntroductionModule.EagerSingletons());
		component.inject(new CryptoModule.EagerSingletons());
		component.inject(new ContactModule.EagerSingletons());
		component.inject(new TransportModule.EagerSingletons());
		component.inject(new SyncModule.EagerSingletons());
		component.inject(new SystemModule.EagerSingletons());
		component.inject(new PropertiesModule.EagerSingletons());
	}
}

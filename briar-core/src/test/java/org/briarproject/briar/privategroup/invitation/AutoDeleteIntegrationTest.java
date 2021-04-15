package org.briarproject.briar.privategroup.invitation;

import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.db.DatabaseComponent;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.briar.api.autodelete.event.ConversationMessagesDeletedEvent;
import org.briarproject.briar.api.conversation.ConversationManager.ConversationClient;
import org.briarproject.briar.api.privategroup.GroupMessage;
import org.briarproject.briar.api.privategroup.PrivateGroup;
import org.briarproject.briar.api.privategroup.PrivateGroupManager;
import org.briarproject.briar.api.privategroup.event.GroupInvitationResponseReceivedEvent;
import org.briarproject.briar.api.privategroup.invitation.GroupInvitationManager;
import org.briarproject.briar.api.privategroup.invitation.GroupInvitationRequest;
import org.briarproject.briar.api.privategroup.invitation.GroupInvitationResponse;
import org.briarproject.briar.autodelete.AbstractAutoDeleteTest;
import org.briarproject.briar.test.BriarIntegrationTestComponent;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;

import static org.briarproject.bramble.api.cleanup.CleanupManager.BATCH_DELAY_MS;
import static org.briarproject.briar.api.autodelete.AutoDeleteConstants.MIN_AUTO_DELETE_TIMER_MS;
import static org.briarproject.briar.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.briarproject.briar.test.TestEventListener.assertEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AutoDeleteIntegrationTest extends AbstractAutoDeleteTest {

	private PrivateGroup privateGroup;
	private PrivateGroupManager groupManager0;
	private GroupInvitationManager groupInvitationManager0,
			groupInvitationManager1;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		groupManager0 = c0.getPrivateGroupManager();
		groupInvitationManager0 = c0.getGroupInvitationManager();
		groupInvitationManager1 = c1.getGroupInvitationManager();
		privateGroup = addPrivateGroup("Testgroup", startTime);
	}

	@Override
	protected ConversationClient getConversationClient(
			BriarIntegrationTestComponent component) {
		return component.getGroupInvitationManager();
	}

	@Test
	public void testInvitationAutoDecline() throws Exception {
		setAutoDeleteTimer(c0, contact1From0.getId(), MIN_AUTO_DELETE_TIMER_MS);

		// Send invitation
		sendInvitation(privateGroup, contact1From0.getId(), "Join this!");

		// The message should have been added to 0's view of the conversation
		assertGroupCount(c0, contactId1From0, 1, 0);
		forEachHeader(c0, contactId1From0, 1, h -> {
			// The message should have the expected timer
			assertEquals(MIN_AUTO_DELETE_TIMER_MS, h.getAutoDeleteTimer());
		});

		// Sync the message to 1
		sync0To1(1, true);
		// Sync the ack to 0 - this starts 0's timer
		ack1To0(1);
		waitForEvents(c0);

		// The message should have been added to 1's view of the conversation
		assertGroupCount(c1, contactId0From1, 1, 1);
		forEachHeader(c1, contactId0From1, 1, h -> {
			// The message should have the expected timer
			assertEquals(MIN_AUTO_DELETE_TIMER_MS, h.getAutoDeleteTimer());
		});

		// Both peers should be using the new timer
		assertEquals(MIN_AUTO_DELETE_TIMER_MS,
				getAutoDeleteTimer(c0, contactId1From0));
		assertEquals(MIN_AUTO_DELETE_TIMER_MS,
				getAutoDeleteTimer(c1, contactId0From1));

		// Before 0's timer elapses, both peers should still see the message
		long timerLatency = MIN_AUTO_DELETE_TIMER_MS + BATCH_DELAY_MS;
		c0.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		c1.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		assertGroupCount(c0, contactId1From0, 1, 0);
		assertEquals(1, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 1, 1);
		assertEquals(1, getMessageHeaders(c1, contactId0From1).size());

		// When 0's timer has elapsed, the message should be deleted from 0's
		// view of the conversation but 1 should still see the message
		ConversationMessagesDeletedEvent event = assertEvent(c0,
				ConversationMessagesDeletedEvent.class, () ->
						c0.getTimeTravel().addCurrentTimeMillis(1)
		);
		c1.getTimeTravel().addCurrentTimeMillis(1);

		// assert that the proper event got broadcast
		assertEquals(contactId1From0, event.getContactId());

		assertGroupCount(c0, contactId1From0, 0, 0);
		assertEquals(0, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 1, 1);
		assertEquals(1, getMessageHeaders(c1, contactId0From1).size());

		// 1 marks the message as read - this starts 1's timer
		final MessageId messageId0 =
				getMessageHeaders(c1, contactId0From1).get(0).getId();
		markMessageRead(c1, contact0From1, messageId0);
		assertGroupCount(c1, contactId0From1, 1, 0);

		// Before 1's timer elapses, 1 should still see the message
		c0.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		c1.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		assertGroupCount(c0, contactId1From0, 0, 0);
		assertEquals(0, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 1, 0);
		assertEquals(1, getMessageHeaders(c1, contactId0From1).size());

		// When 1's timer has elapsed, the message should be deleted from 1's
		// view of the conversation and the invitation auto-declined
		c0.getTimeTravel().addCurrentTimeMillis(1);
		GroupInvitationResponseReceivedEvent e = assertEvent(c1,
				GroupInvitationResponseReceivedEvent.class, () ->
						c1.getTimeTravel().addCurrentTimeMillis(1)
		);
		// assert that the proper event got broadcast
		assertEquals(contactId0From1, e.getContactId());

		assertGroupCount(c0, contactId1From0, 0, 0);
		assertEquals(0, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 1, 0);
		forEachHeader(c1, contactId0From1, 1, h -> {
			// The only message is not the same as before, but declined response
			assertNotEquals(messageId0, h.getId());
			assertTrue(h instanceof GroupInvitationResponse);
			assertEquals(h.getId(), e.getMessageHeader().getId());
			assertFalse(((GroupInvitationResponse) h).wasAccepted());
			assertTrue(((GroupInvitationResponse) h).isAutoDecline());
			// The auto-decline message should have the expected timer
			assertEquals(MIN_AUTO_DELETE_TIMER_MS,
					h.getAutoDeleteTimer());
		});

		// Sync the auto-decline message to 0
		sync1To0(1, true);
		// Sync the ack to 1 - this starts 1's timer
		ack0To1(1);
		waitForEvents(c1);

		// 0 can invite 1 again
		assertTrue(groupInvitationManager0
				.isInvitationAllowed(contact1From0, privateGroup.getId()));

		// Before 1's timer elapses, 1 should still see the auto-decline message
		c0.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		c1.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		assertGroupCount(c0, contactId1From0, 1, 1);
		assertEquals(1, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 1, 0);
		assertEquals(1, getMessageHeaders(c1, contactId0From1).size());
		// When 1's timer has elapsed, the auto-decline message should be
		// deleted from 1's view of the conversation
		c0.getTimeTravel().addCurrentTimeMillis(1);
		c1.getTimeTravel().addCurrentTimeMillis(1);
		assertGroupCount(c0, contactId1From0, 1, 1);
		assertEquals(1, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 0, 0);
		assertEquals(0, getMessageHeaders(c1, contactId0From1).size());

		// 0 marks the message as read - this starts 0's timer
		MessageId messageId1 =
				getMessageHeaders(c0, contactId1From0).get(0).getId();
		markMessageRead(c0, contact1From0, messageId1);
		assertGroupCount(c0, contactId1From0, 1, 0);

		// Before 0's timer elapses, 0 should still see the message
		c0.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		assertGroupCount(c0, contactId1From0, 1, 0);
		assertEquals(1, getMessageHeaders(c0, contactId1From0).size());

		// When 0's timer has elapsed, the message should be deleted from 0's
		// view of the conversation
		c0.getTimeTravel().addCurrentTimeMillis(1);
		assertGroupCount(c0, contactId1From0, 0, 0);
		assertEquals(0, getMessageHeaders(c0, contactId1From0).size());

		// 0 can invite 1 again and really does invite
		assertTrue(groupInvitationManager0
				.isInvitationAllowed(contact1From0, privateGroup.getId()));
		sendInvitation(privateGroup, contact1From0.getId(),
				"Join this faster please!");
		sync0To1(1, true);
		assertGroupCount(c1, contactId0From1, 1, 1);
	}

	@Test
	public void testAutoDeleteDoesNotRemoveOtherSessions() throws Exception {
		PrivateGroup pg = addPrivateGroup("Another one", startTime + 1);

		// Send invitation for another group without timer
		sendInvitation(pg, contact1From0.getId(), null);
		sync0To1(1, true);
		ack1To0(1);
		waitForEvents(c0);

		// The message should have been added the views of the conversation
		assertGroupCount(c0, contactId1From0, 1, 0);
		assertGroupCount(c1, contactId0From1, 1, 1);
		// The message should have no timer for either peer
		forEachHeader(c0, contactId1From0, 1, h ->
				assertEquals(NO_AUTO_DELETE_TIMER, h.getAutoDeleteTimer()));
		forEachHeader(c1, contactId0From1, 1, h ->
				assertEquals(NO_AUTO_DELETE_TIMER, h.getAutoDeleteTimer()));

		// enable timer
		setAutoDeleteTimer(c0, contact1From0.getId(), MIN_AUTO_DELETE_TIMER_MS);

		// Send invitation, ACK it and check group counts
		sendInvitation(privateGroup, contact1From0.getId(), "Join this!");
		sync0To1(1, true);
		ack1To0(1);
		waitForEvents(c0);
		assertGroupCount(c0, contactId1From0, 2, 0);
		assertGroupCount(c1, contactId0From1, 2, 2);

		// Both peers should be using the new timer
		assertEquals(MIN_AUTO_DELETE_TIMER_MS,
				getAutoDeleteTimer(c0, contactId1From0));
		assertEquals(MIN_AUTO_DELETE_TIMER_MS,
				getAutoDeleteTimer(c1, contactId0From1));

		// Before 0's timer elapses, both peers should still see the message
		long timerLatency = MIN_AUTO_DELETE_TIMER_MS + BATCH_DELAY_MS;
		c0.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		c1.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		assertGroupCount(c0, contactId1From0, 2, 0);
		assertEquals(2, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 2, 2);
		assertEquals(2, getMessageHeaders(c1, contactId0From1).size());

		// When 0's timer has elapsed, the message should be deleted from 0's
		// view of the conversation but 1 should still see the message
		c0.getTimeTravel().addCurrentTimeMillis(1);
		c1.getTimeTravel().addCurrentTimeMillis(1);
		assertGroupCount(c0, contactId1From0, 1, 0);
		assertEquals(1, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 2, 2);
		assertEquals(2, getMessageHeaders(c1, contactId0From1).size());

		// 1 marks all the message as read - this starts 1's timer for 2nd msg
		forEachHeader(c1, contactId0From1, 2, h -> {
			try {
				markMessageRead(c1, contact0From1, h.getId());
			} catch (Exception e) {
				fail();
			}
		});
		assertGroupCount(c1, contactId0From1, 2, 0);

		// Before 1's timer elapses, 1 should still see the message
		c0.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		c1.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		assertGroupCount(c0, contactId1From0, 1, 0);
		assertEquals(1, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 2, 0);
		assertEquals(2, getMessageHeaders(c1, contactId0From1).size());

		// When 1's timer has elapsed, the message should be deleted from 1's
		// view of the conversation and the invitation auto-declined
		c0.getTimeTravel().addCurrentTimeMillis(1);
		GroupInvitationResponseReceivedEvent event = assertEvent(c1,
				GroupInvitationResponseReceivedEvent.class,
				() -> c1.getTimeTravel().addCurrentTimeMillis(1)
		);
		// assert that the proper event got broadcast
		assertEquals(contactId0From1, event.getContactId());
		assertGroupCount(c0, contactId1From0, 1, 0);
		assertEquals(1, getMessageHeaders(c0, contactId1From0).size());
		// 1's total count is still 2, because of the added auto-decline
		assertGroupCount(c1, contactId0From1, 2, 0);
		forEachHeader(c1, contactId0From1, 2, h -> {
			if (h instanceof GroupInvitationRequest) {
				// the request is for the first invitation
				assertEquals(pg.getId(),
						((GroupInvitationRequest) h).getNameable().getId());
			} else {
				assertTrue(h instanceof GroupInvitationResponse);
				GroupInvitationResponse r = (GroupInvitationResponse) h;
				assertEquals(h.getId(), event.getMessageHeader().getId());
				// is auto-decline for 2nd invitation
				assertEquals(privateGroup.getId(), r.getShareableId());
				assertTrue(r.isAutoDecline());
				assertFalse(r.wasAccepted());
			}
		});

		// Sync the auto-decline message to 0
		sync1To0(1, true);
		// Sync the ack to 1 - this starts 1's timer
		ack0To1(1);
		waitForEvents(c1);
		// 0 marks the message as read - this starts 0's timer
		GroupInvitationResponse autoDeclineMessage = (GroupInvitationResponse)
				getMessageHeaders(c0, contactId1From0).get(1);
		markMessageRead(c0, contact1From0, autoDeclineMessage.getId());
		assertGroupCount(c0, contactId1From0, 2, 0);
		assertGroupCount(c1, contactId0From1, 2, 0);

		// Timer of auto-decline elapses for both peers at the same time
		c0.getTimeTravel().addCurrentTimeMillis(timerLatency);
		c1.getTimeTravel().addCurrentTimeMillis(timerLatency);
		assertGroupCount(c0, contactId1From0, 1, 0);
		assertGroupCount(c1, contactId0From1, 1, 0);

		// 1 responds to first invitation (that had no timer)
		groupInvitationManager1.respondToInvitation(contactId0From1, pg, true);
		// Sync the accept response message to 0
		sync1To0(1, true);
		// Sync the ack (and creator's join messages (group + protocol) to 1
		// this starts 1's timer
		sync0To1(2, true);
		waitForEvents(c1);
		assertGroupCount(c0, contactId1From0, 2, 1);
		assertGroupCount(c1, contactId0From1, 2, 0);
		forEachHeader(c1, contactId0From1, 2, h -> {
			if (h instanceof GroupInvitationRequest) {
				// the request is for the first invitation
				assertEquals(pg.getId(),
						((GroupInvitationRequest) h).getNameable().getId());
			} else {
				assertTrue(h instanceof GroupInvitationResponse);
				GroupInvitationResponse r = (GroupInvitationResponse) h;
				// is accept for 1nd invitation
				assertEquals(pg.getId(), r.getShareableId());
				assertFalse(r.isAutoDecline());
				assertTrue(r.wasAccepted());
			}
		});

		// Before 1's timer elapses, 1 should still see the message
		c0.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		c1.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		assertGroupCount(c0, contactId1From0, 2, 1);
		assertEquals(2, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 2, 0);
		assertEquals(2, getMessageHeaders(c1, contactId0From1).size());

		// When 1's timer has elapsed, the message should be deleted from 1's
		// view of the conversation
		c0.getTimeTravel().addCurrentTimeMillis(1);
		c1.getTimeTravel().addCurrentTimeMillis(1);
		assertGroupCount(c0, contactId1From0, 2, 1);
		assertEquals(2, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 1, 0);
		forEachHeader(c1, contactId0From1, 1, h -> {
			assertTrue(h instanceof GroupInvitationRequest);
			assertTrue(((GroupInvitationRequest) h).wasAnswered());
			assertTrue(((GroupInvitationRequest) h).canBeOpened());
		});

		// 0 reads all messages
		forEachHeader(c0, contactId1From0, 2, h -> {
			try {
				if (!h.isRead()) markMessageRead(c0, contact1From0, h.getId());
			} catch (Exception e) {
				fail();
			}
		});
		assertGroupCount(c0, contactId1From0, 2, 0);

		// Before 0's timer elapses, 0 should still see the messages
		c0.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		c1.getTimeTravel().addCurrentTimeMillis(timerLatency - 1);
		assertGroupCount(c0, contactId1From0, 2, 0);
		assertGroupCount(c1, contactId0From1, 1, 0);

		// When 0's timer has elapsed, the messages should be deleted from 0's
		// view of the conversation, only the initial invitation remains
		c0.getTimeTravel().addCurrentTimeMillis(1);
		c1.getTimeTravel().addCurrentTimeMillis(1);
		assertGroupCount(c0, contactId1From0, 1, 0);
		assertEquals(1, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 1, 0);
		assertEquals(1, getMessageHeaders(c1, contactId0From1).size());

		// 1 joined the PrivateGroup
		assertEquals(pg,
				c1.getPrivateGroupManager().getPrivateGroup(pg.getId()));
		assertFalse(groupInvitationManager0
				.isInvitationAllowed(contact1From0, pg.getId()));
	}

	@Test
	public void testResponseAfterSenderDeletedInvitation() throws Exception {
		setAutoDeleteTimer(c0, contact1From0.getId(), MIN_AUTO_DELETE_TIMER_MS);

		// Send invitation
		sendInvitation(privateGroup, contact1From0.getId(), "Join this!");
		assertGroupCount(c0, contactId1From0, 1, 0);

		// Sync the message to 1
		sync0To1(1, true);
		// Sync the ack to 0 - this starts 0's timer
		ack1To0(1);
		waitForEvents(c0);
		assertGroupCount(c1, contactId0From1, 1, 1);

		// When 0's timer has elapsed, the message should be deleted from 0's
		// view of the conversation but 1 should still see the message
		long timerLatency = MIN_AUTO_DELETE_TIMER_MS + BATCH_DELAY_MS;
		c0.getTimeTravel().addCurrentTimeMillis(timerLatency);
		c1.getTimeTravel().addCurrentTimeMillis(timerLatency);
		assertGroupCount(c0, contactId1From0, 0, 0);
		assertEquals(0, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 1, 1);
		assertEquals(1, getMessageHeaders(c1, contactId0From1).size());

		// 1 marks message as read - this starts 1's timer
		markMessageRead(c1, contact0From1,
				getMessageHeaders(c1, contactId0From1).get(0).getId());

		// 1 responds to invitation
		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup, false);
		// Sync the decline response message to 0
		sync1To0(1, true);
		// Sync the ack to 1 - this starts 1's timer
		ack0To1(1);
		waitForEvents(c1);
		assertGroupCount(c0, contactId1From0, 1, 1);
		assertGroupCount(c1, contactId0From1, 2, 0);

		// 0 marks the message as read - this starts 0's timer
		GroupInvitationResponse message1 = (GroupInvitationResponse)
				getMessageHeaders(c0, contactId1From0).get(0);
		markMessageRead(c0, contact1From0, message1.getId());
		assertGroupCount(c0, contactId1From0, 1, 0);
		assertGroupCount(c1, contactId0From1, 2, 0);

		// both peers delete all messages after their timers expire
		c0.getTimeTravel().addCurrentTimeMillis(timerLatency);
		c1.getTimeTravel().addCurrentTimeMillis(timerLatency);
		assertGroupCount(c0, contactId1From0, 0, 0);
		assertEquals(0, getMessageHeaders(c0, contactId1From0).size());
		assertGroupCount(c1, contactId0From1, 0, 0);
		assertEquals(0, getMessageHeaders(c1, contactId0From1).size());
	}

	private PrivateGroup addPrivateGroup(String name, long timestamp)
			throws DbException {
		PrivateGroup pg = privateGroupFactory.createPrivateGroup(name, author0);
		GroupMessage joinMsg0 = groupMessageFactory
				.createJoinMessage(pg.getId(), timestamp, author0);
		groupManager0.addPrivateGroup(pg, joinMsg0, true);
		return pg;
	}

	private void sendInvitation(PrivateGroup pg, ContactId contactId,
			@Nullable String text) throws DbException {
		DatabaseComponent db0 = c0.getDatabaseComponent();
		long timestamp = db0.transactionWithResult(true, txn ->
				c0.getConversationManager()
						.getTimestampForOutgoingMessage(txn, contactId));
		byte[] signature = groupInvitationFactory.signInvitation(contact1From0,
				pg.getId(), timestamp, author0.getPrivateKey());
		long timer = getAutoDeleteTimer(c0, contactId, timestamp);
		groupInvitationManager0.sendInvitation(pg.getId(), contactId, text,
				timestamp, signature, timer);
	}
}

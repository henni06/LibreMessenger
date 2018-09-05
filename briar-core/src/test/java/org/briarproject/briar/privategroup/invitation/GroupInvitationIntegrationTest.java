package org.briarproject.briar.privategroup.invitation;

import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.sync.Group;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.bramble.test.TestDatabaseModule;
import org.briarproject.briar.api.client.ProtocolStateException;
import org.briarproject.briar.api.messaging.PrivateMessageHeader;
import org.briarproject.briar.api.privategroup.GroupMessage;
import org.briarproject.briar.api.privategroup.PrivateGroup;
import org.briarproject.briar.api.privategroup.PrivateGroupManager;
import org.briarproject.briar.api.privategroup.invitation.GroupInvitationItem;
import org.briarproject.briar.api.privategroup.invitation.GroupInvitationManager;
import org.briarproject.briar.api.privategroup.invitation.GroupInvitationRequest;
import org.briarproject.briar.api.privategroup.invitation.GroupInvitationResponse;
import org.briarproject.briar.test.BriarIntegrationTest;
import org.briarproject.briar.test.BriarIntegrationTestComponent;
import org.briarproject.briar.test.DaggerBriarIntegrationTestComponent;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import javax.annotation.Nullable;

import static org.briarproject.briar.test.BriarTestUtils.assertGroupCount;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GroupInvitationIntegrationTest
		extends BriarIntegrationTest<BriarIntegrationTestComponent> {

	private PrivateGroup privateGroup0;
	private PrivateGroupManager groupManager0, groupManager1;
	private GroupInvitationManager groupInvitationManager0,
			groupInvitationManager1;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		groupManager0 = c0.getPrivateGroupManager();
		groupManager1 = c1.getPrivateGroupManager();
		groupInvitationManager0 = c0.getGroupInvitationManager();
		groupInvitationManager1 = c1.getGroupInvitationManager();

		privateGroup0 =
				privateGroupFactory.createPrivateGroup("Testgroup", author0);
		long joinTime = clock.currentTimeMillis();
		GroupMessage joinMsg0 = groupMessageFactory
				.createJoinMessage(privateGroup0.getId(), joinTime, author0);
		groupManager0.addPrivateGroup(privateGroup0, joinMsg0, true);
	}

	@Override
	protected void createComponents() {
		BriarIntegrationTestComponent component =
				DaggerBriarIntegrationTestComponent.builder().build();
		component.inject(this);

		c0 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseModule(new TestDatabaseModule(t0Dir)).build();
		injectEagerSingletons(c0);

		c1 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseModule(new TestDatabaseModule(t1Dir)).build();
		injectEagerSingletons(c1);

		c2 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseModule(new TestDatabaseModule(t2Dir)).build();
		injectEagerSingletons(c2);
	}

	@Test
	public void testSendInvitation() throws Exception {
		long timestamp = clock.currentTimeMillis();
		String msg = "Hi!";
		sendInvitation(timestamp, msg);

		sync0To1(1, true);

		Collection<GroupInvitationItem> invitations =
				groupInvitationManager1.getInvitations();
		assertEquals(1, invitations.size());
		GroupInvitationItem item = invitations.iterator().next();
		assertEquals(contact0From1, item.getCreator());
		assertEquals(privateGroup0, item.getShareable());
		assertEquals(privateGroup0.getId(), item.getId());
		assertEquals(privateGroup0.getName(), item.getName());
		assertFalse(item.isSubscribed());

		Collection<PrivateMessageHeader> messages =
				withinTransactionReturns(db1, txn -> groupInvitationManager1
						.getMessageHeaders(txn, contactId0From1));
		assertEquals(1, messages.size());
		GroupInvitationRequest request =
				(GroupInvitationRequest) messages.iterator().next();
		assertEquals(msg, request.getMessage());
		assertEquals(author0, request.getObject().getCreator());
		assertEquals(timestamp, request.getTimestamp());
		assertEquals(privateGroup0.getName(), request.getObject().getName());
		assertFalse(request.isLocal());
		assertFalse(request.isRead());
		assertFalse(request.doesExist());
	}

	@Test
	public void testInvitationDecline() throws Exception {
		long timestamp = clock.currentTimeMillis();
		sendInvitation(timestamp, null);

		sync0To1(1, true);
		assertFalse(groupInvitationManager1.getInvitations().isEmpty());

		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, false);

		Collection<PrivateMessageHeader> messages =
				withinTransactionReturns(db1, txn -> groupInvitationManager1
						.getMessageHeaders(txn, contactId0From1));
		assertEquals(2, messages.size());
		boolean foundResponse = false;
		for (PrivateMessageHeader m : messages) {
			if (m instanceof GroupInvitationResponse) {
				foundResponse = true;
				GroupInvitationResponse response = (GroupInvitationResponse) m;
				assertEquals(privateGroup0, response.getObject());
				assertTrue(response.isLocal());
				assertFalse(response.wasAccepted());
			}
		}
		assertTrue(foundResponse);

		sync1To0(1, true);

		messages = withinTransactionReturns(db0, txn -> groupInvitationManager0
				.getMessageHeaders(txn, contactId1From0));
		assertEquals(2, messages.size());
		foundResponse = false;
		for (PrivateMessageHeader m : messages) {
			if (m instanceof GroupInvitationResponse) {
				foundResponse = true;
				GroupInvitationResponse response = (GroupInvitationResponse) m;
				assertEquals(privateGroup0, response.getObject());
				assertFalse(response.isLocal());
				assertFalse(response.wasAccepted());
			}
		}
		assertTrue(foundResponse);

		// no invitations are open
		assertTrue(groupInvitationManager1.getInvitations().isEmpty());
		// no groups were added
		assertEquals(0, groupManager1.getPrivateGroups().size());
	}

	@Test
	public void testInvitationAccept() throws Exception {
		long timestamp = clock.currentTimeMillis();
		sendInvitation(timestamp, null);

		sync0To1(1, true);
		assertFalse(groupInvitationManager1.getInvitations().isEmpty());

		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, true);

		Collection<PrivateMessageHeader> messages =
				withinTransactionReturns(db1, txn -> groupInvitationManager1
						.getMessageHeaders(txn, contactId0From1));
		assertEquals(2, messages.size());
		boolean foundResponse = false;
		for (PrivateMessageHeader m : messages) {
			if (m instanceof GroupInvitationResponse) {
				foundResponse = true;
				GroupInvitationResponse response = (GroupInvitationResponse) m;
				assertEquals(privateGroup0, response.getObject());
				assertTrue(response.wasAccepted());
			} else {
				GroupInvitationRequest request = (GroupInvitationRequest) m;
				assertEquals(privateGroup0, request.getObject());
				assertTrue(request.doesExist());
			}
		}
		assertTrue(foundResponse);

		sync1To0(1, true);

		messages = withinTransactionReturns(db1, txn -> groupInvitationManager0
				.getMessageHeaders(txn, contactId1From0));
		assertEquals(2, messages.size());
		foundResponse = false;
		for (PrivateMessageHeader m : messages) {
			if (m instanceof GroupInvitationResponse) {
				foundResponse = true;
				GroupInvitationResponse response = (GroupInvitationResponse) m;
				assertEquals(privateGroup0, response.getObject());
				assertTrue(response.wasAccepted());
			}
		}
		assertTrue(foundResponse);

		// no invitations are open
		assertTrue(groupInvitationManager1.getInvitations().isEmpty());
		// group was added
		Collection<PrivateGroup> groups = groupManager1.getPrivateGroups();
		assertEquals(1, groups.size());
		assertEquals(privateGroup0, groups.iterator().next());
	}

	@Test
	public void testGroupCount() throws Exception {
		long timestamp = clock.currentTimeMillis();
		sendInvitation(timestamp, null);

		// 0 has one read outgoing message
		Group g1 = groupInvitationManager0.getContactGroup(contact1From0);
		assertGroupCount(messageTracker0, g1.getId(), 1, 0, timestamp);

		sync0To1(1, true);

		// 1 has one unread message
		Group g0 = groupInvitationManager1.getContactGroup(contact0From1);
		assertGroupCount(messageTracker1, g0.getId(), 1, 1, timestamp);
		PrivateMessageHeader m = withinTransactionReturns(db1,
				txn -> groupInvitationManager1.getMessageHeaders(txn, contactId0From1)
						.iterator().next());

		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, true);

		// 1 has two messages, one still unread
		assertGroupCount(messageTracker1, g0.getId(), 2, 1);

		// now all messages should be read
		groupInvitationManager1.setReadFlag(g0.getId(), m.getId(), true);
		assertGroupCount(messageTracker1, g0.getId(), 2, 0);

		sync1To0(1, true);

		// now 0 has two messages, one of them unread
		assertGroupCount(messageTracker0, g1.getId(), 2, 1);
	}

	@Test
	public void testMultipleInvitations() throws Exception {
		sendInvitation(clock.currentTimeMillis(), null);

		// invitation is not allowed before the first hasn't been answered
		assertFalse(groupInvitationManager0
				.isInvitationAllowed(contact1From0, privateGroup0.getId()));

		// deliver invitation and response
		sync0To1(1, true);
		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, false);
		sync1To0(1, true);

		// after invitation was declined, inviting again is possible
		assertTrue(groupInvitationManager0
				.isInvitationAllowed(contact1From0, privateGroup0.getId()));

		// send and accept the second invitation
		sendInvitation(clock.currentTimeMillis(), "Second Invitation");
		sync0To1(1, true);
		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, true);
		sync1To0(1, true);

		// invitation is not allowed since the member joined the group now
		assertFalse(groupInvitationManager0
				.isInvitationAllowed(contact1From0, privateGroup0.getId()));

		// don't allow another invitation request
		try {
			sendInvitation(clock.currentTimeMillis(), "Third Invitation");
			fail();
		} catch (ProtocolStateException e) {
			// expected
		}
	}

	@Test(expected = ProtocolStateException.class)
	public void testInvitationsWithSameTimestamp() throws Exception {
		long timestamp = clock.currentTimeMillis();
		sendInvitation(timestamp, null);
		sync0To1(1, true);

		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, false);
		sync1To0(1, true);

		sendInvitation(timestamp, "Second Invitation");
		sync0To1(1, true);

		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, true);
	}

	@Test(expected = ProtocolStateException.class)
	public void testCreatorLeavesBeforeInvitationAccepted() throws Exception {
		// Creator invites invitee to join group
		sendInvitation(clock.currentTimeMillis(), null);

		// Creator's invite message is delivered to invitee
		sync0To1(1, true);

		// Creator leaves group
		assertEquals(1, groupManager0.getPrivateGroups().size());
		groupManager0.removePrivateGroup(privateGroup0.getId());
		assertEquals(0, groupManager0.getPrivateGroups().size());

		// Creator's leave message is delivered to invitee
		sync0To1(1, true);

		// Invitee accepts invitation, but it's no longer open - exception is
		// thrown as the action has failed
		assertEquals(0, groupManager1.getPrivateGroups().size());
		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, true);
	}

	@Test
	public void testCreatorLeavesBeforeInvitationDeclined() throws Exception {
		// Creator invites invitee to join group
		sendInvitation(clock.currentTimeMillis(), null);

		// Creator's invite message is delivered to invitee
		sync0To1(1, true);

		// Creator leaves group
		assertEquals(1, groupManager0.getPrivateGroups().size());
		groupManager0.removePrivateGroup(privateGroup0.getId());
		assertEquals(0, groupManager0.getPrivateGroups().size());

		// Creator's leave message is delivered to invitee
		sync0To1(1, true);

		// invitee should have no more open invitations
		assertTrue(groupInvitationManager1.getInvitations().isEmpty());

		// Invitee declines invitation, but it's no longer open - no exception
		// as the action has succeeded
		assertEquals(0, groupManager1.getPrivateGroups().size());
		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, false);
	}

	@Test
	public void testCreatorLeavesConcurrentlyWithInvitationAccepted()
			throws Exception {
		// Creator invites invitee to join group
		sendInvitation(clock.currentTimeMillis(), null);

		// Creator's invite message is delivered to invitee
		sync0To1(1, true);

		// Creator leaves group
		assertEquals(1, groupManager0.getPrivateGroups().size());
		groupManager0.removePrivateGroup(privateGroup0.getId());
		assertEquals(0, groupManager0.getPrivateGroups().size());

		// Invitee accepts invitation
		assertEquals(0, groupManager1.getPrivateGroups().size());
		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, true);
		assertEquals(1, groupManager1.getPrivateGroups().size());
		assertFalse(groupManager1.isDissolved(privateGroup0.getId()));

		// Invitee's join message is delivered to creator
		sync1To0(1, true);

		// Creator's leave message is delivered to invitee
		sync0To1(1, true);

		// Group is marked as dissolved
		assertTrue(groupManager1.isDissolved(privateGroup0.getId()));
	}

	@Test
	public void testCreatorLeavesConcurrentlyWithInvitationDeclined()
			throws Exception {
		// Creator invites invitee to join group
		sendInvitation(clock.currentTimeMillis(), null);

		// Creator's invite message is delivered to invitee
		sync0To1(1, true);

		// Creator leaves group
		assertEquals(1, groupManager0.getPrivateGroups().size());
		groupManager0.removePrivateGroup(privateGroup0.getId());
		assertEquals(0, groupManager0.getPrivateGroups().size());

		// Invitee declines invitation
		assertEquals(0, groupManager1.getPrivateGroups().size());
		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, false);
		assertEquals(0, groupManager1.getPrivateGroups().size());

		// Invitee's leave message is delivered to creator
		sync1To0(1, true);

		// Creator's leave message is delivered to invitee
		sync0To1(1, true);
	}

	@Test
	public void testCreatorLeavesConcurrentlyWithMemberLeaving()
			throws Exception {
		// Creator invites invitee to join group
		sendInvitation(clock.currentTimeMillis(), null);

		// Creator's invite message is delivered to invitee
		sync0To1(1, true);

		// Invitee responds to invitation
		assertEquals(0, groupManager1.getPrivateGroups().size());
		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, true);
		assertEquals(1, groupManager1.getPrivateGroups().size());

		// Invitee's join message is delivered to creator
		sync1To0(1, true);

		// Creator leaves group
		assertEquals(1, groupManager0.getPrivateGroups().size());
		groupManager0.removePrivateGroup(privateGroup0.getId());
		assertEquals(0, groupManager0.getPrivateGroups().size());

		// Invitee leaves group
		groupManager1.removePrivateGroup(privateGroup0.getId());
		assertEquals(0, groupManager1.getPrivateGroups().size());

		// Creator's leave message is delivered to invitee
		sync0To1(1, true);

		// Invitee's leave message is delivered to creator
		sync1To0(1, true);
	}

	@Test(expected = AssertionError.class)
	public void testDeleteOnlyInvitationFails() throws Exception {
		// send invitation
		sendInvitation(clock.currentTimeMillis(), null);
		sync0To1(1, true);

		// save MessageId of invitation
		Collection<PrivateMessageHeader> messages =
				withinTransactionReturns(db1, txn -> groupInvitationManager1
						.getMessageHeaders(txn, contactId0From1));
		assertEquals(1, messages.size());
		MessageId inviteId = messages.iterator().next().getId();

		// decline invitation
		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, false);

		// we should have an invitation and a decline message
		messages = withinTransactionReturns(db1, txn -> groupInvitationManager1
				.getMessageHeaders(txn, contactId0From1));
		assertEquals(2, messages.size());

		// delete only invitation
		withinTransaction(db1, txn -> {
			db1.deleteMessage(txn, inviteId);
			db1.deleteMessageMetadata(txn, inviteId);
			// This should fail
			groupInvitationManager1.getMessageHeaders(txn, contactId0From1);
		});

	}

	private void sendInvitation(long timestamp, @Nullable String msg) throws
			DbException {
		byte[] signature = groupInvitationFactory.signInvitation(contact1From0,
				privateGroup0.getId(), timestamp, author0.getPrivateKey());
		groupInvitationManager0
				.sendInvitation(privateGroup0.getId(), contactId1From0, msg,
						timestamp, signature);
	}

}

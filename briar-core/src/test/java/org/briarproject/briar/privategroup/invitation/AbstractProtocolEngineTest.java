package org.briarproject.briar.privategroup.invitation;

import org.briarproject.bramble.api.client.ClientHelper;
import org.briarproject.bramble.api.contact.Contact;
import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.data.BdfDictionary;
import org.briarproject.bramble.api.data.BdfEntry;
import org.briarproject.bramble.api.db.DatabaseComponent;
import org.briarproject.bramble.api.db.Transaction;
import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.identity.IdentityManager;
import org.briarproject.bramble.api.sync.Group;
import org.briarproject.bramble.api.sync.Group.Visibility;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.Message;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.bramble.api.system.Clock;
import org.briarproject.bramble.api.versioning.ClientVersioningManager;
import org.briarproject.bramble.test.BrambleMockTestCase;
import org.briarproject.briar.api.client.MessageTracker;
import org.briarproject.briar.api.privategroup.GroupMessageFactory;
import org.briarproject.briar.api.privategroup.PrivateGroup;
import org.briarproject.briar.api.privategroup.PrivateGroupFactory;
import org.briarproject.briar.api.privategroup.PrivateGroupManager;
import org.jmock.Expectations;

import static org.briarproject.bramble.api.identity.AuthorConstants.MAX_SIGNATURE_LENGTH;
import static org.briarproject.bramble.api.sync.Group.Visibility.SHARED;
import static org.briarproject.bramble.test.TestUtils.getContact;
import static org.briarproject.bramble.test.TestUtils.getGroup;
import static org.briarproject.bramble.test.TestUtils.getMessage;
import static org.briarproject.bramble.test.TestUtils.getRandomBytes;
import static org.briarproject.bramble.test.TestUtils.getRandomId;
import static org.briarproject.bramble.util.StringUtils.getRandomString;
import static org.briarproject.briar.api.privategroup.PrivateGroupConstants.GROUP_SALT_LENGTH;
import static org.briarproject.briar.api.privategroup.PrivateGroupConstants.MAX_GROUP_INVITATION_TEXT_LENGTH;
import static org.briarproject.briar.api.privategroup.PrivateGroupConstants.MAX_GROUP_NAME_LENGTH;
import static org.briarproject.briar.api.privategroup.PrivateGroupManager.CLIENT_ID;
import static org.briarproject.briar.api.privategroup.PrivateGroupManager.MAJOR_VERSION;
import static org.briarproject.briar.privategroup.invitation.GroupInvitationConstants.GROUP_KEY_CONTACT_ID;
import static org.briarproject.briar.privategroup.invitation.MessageType.ABORT;
import static org.briarproject.briar.privategroup.invitation.MessageType.INVITE;
import static org.briarproject.briar.privategroup.invitation.MessageType.JOIN;
import static org.briarproject.briar.privategroup.invitation.MessageType.LEAVE;
import static org.junit.Assert.assertEquals;

abstract class AbstractProtocolEngineTest extends BrambleMockTestCase {

	final DatabaseComponent db = context.mock(DatabaseComponent.class);
	final ClientHelper clientHelper = context.mock(ClientHelper.class);
	final ClientVersioningManager clientVersioningManager =
			context.mock(ClientVersioningManager.class);
	final PrivateGroupFactory privateGroupFactory =
			context.mock(PrivateGroupFactory.class);
	final PrivateGroupManager privateGroupManager =
			context.mock(PrivateGroupManager.class);
	final MessageParser messageParser = context.mock(MessageParser.class);
	final GroupMessageFactory groupMessageFactory =
			context.mock(GroupMessageFactory.class);
	final IdentityManager identityManager = context.mock(IdentityManager.class);
	final MessageEncoder messageEncoder = context.mock(MessageEncoder.class);
	final MessageTracker messageTracker = context.mock(MessageTracker.class);
	final Clock clock = context.mock(Clock.class);

	final Transaction txn = new Transaction(null, false);
	final Contact contact = getContact();
	final ContactId contactId = contact.getId();
	final Author author = contact.getAuthor();
	final GroupId contactGroupId = new GroupId(getRandomId());
	final Group privateGroupGroup = getGroup(CLIENT_ID, MAJOR_VERSION);
	final GroupId privateGroupId = privateGroupGroup.getId();
	final PrivateGroup privateGroup = new PrivateGroup(privateGroupGroup,
			getRandomString(MAX_GROUP_NAME_LENGTH), author,
			getRandomBytes(GROUP_SALT_LENGTH));
	final byte[] signature = getRandomBytes(MAX_SIGNATURE_LENGTH);
	final MessageId lastLocalMessageId = new MessageId(getRandomId());
	final MessageId lastRemoteMessageId = new MessageId(getRandomId());
	final Message message = getMessage(contactGroupId);
	final MessageId messageId = message.getId();
	final long messageTimestamp = message.getTimestamp();
	final long inviteTimestamp = messageTimestamp - 1;
	final long localTimestamp = inviteTimestamp - 1;

	final InviteMessage inviteMessage =
			new InviteMessage(new MessageId(getRandomId()), contactGroupId,
					privateGroupId, 0L, privateGroup.getName(),
					privateGroup.getCreator(), privateGroup.getSalt(),
					getRandomString(MAX_GROUP_INVITATION_TEXT_LENGTH),
					signature);
	final JoinMessage joinMessage =
			new JoinMessage(new MessageId(getRandomId()), contactGroupId,
					privateGroupId, 0L, lastRemoteMessageId);
	final LeaveMessage leaveMessage =
			new LeaveMessage(new MessageId(getRandomId()), contactGroupId,
					privateGroupId, 0L, lastRemoteMessageId);
	final AbortMessage abortMessage =
			new AbortMessage(messageId, contactGroupId, privateGroupId,
					inviteTimestamp + 1);

	void assertSessionConstantsUnchanged(Session s1, Session s2) {
		assertEquals(s1.getRole(), s2.getRole());
		assertEquals(s1.getContactGroupId(), s2.getContactGroupId());
		assertEquals(s1.getPrivateGroupId(), s2.getPrivateGroupId());
	}

	void assertSessionRecordedSentMessage(Session s) {
		assertEquals(messageId, s.getLastLocalMessageId());
		assertEquals(lastRemoteMessageId, s.getLastRemoteMessageId());
		assertEquals(messageTimestamp, s.getLocalTimestamp());
		assertEquals(inviteTimestamp, s.getInviteTimestamp());
	}

	void expectGetLocalTimestamp(long time) {
		context.checking(new Expectations() {{
			oneOf(clock).currentTimeMillis();
			will(returnValue(time));
		}});
	}

	void expectSendInviteMessage(String text) throws Exception {
		context.checking(new Expectations() {{
			oneOf(messageEncoder)
					.encodeInviteMessage(contactGroupId, privateGroupId,
							inviteTimestamp, privateGroup.getName(), author,
							privateGroup.getSalt(), text, signature);
			will(returnValue(message));
		}});
		expectSendMessage(INVITE, true);
	}

	void expectSendJoinMessage(JoinMessage m, boolean visible)
			throws Exception {
		expectGetLocalTimestamp(messageTimestamp);
		context.checking(new Expectations() {{
			oneOf(messageEncoder).encodeJoinMessage(m.getContactGroupId(),
					m.getPrivateGroupId(), m.getTimestamp(),
					lastLocalMessageId);
			will(returnValue(message));
		}});
		expectSendMessage(JOIN, visible);
	}

	void expectSendLeaveMessage(boolean visible) throws Exception {
		expectGetLocalTimestamp(messageTimestamp);
		context.checking(new Expectations() {{
			oneOf(messageEncoder)
					.encodeLeaveMessage(contactGroupId, privateGroupId,
							messageTimestamp, lastLocalMessageId);
			will(returnValue(message));
		}});
		expectSendMessage(LEAVE, visible);
	}

	void expectSendAbortMessage() throws Exception {
		expectGetLocalTimestamp(messageTimestamp);
		context.checking(new Expectations() {{
			oneOf(messageEncoder)
					.encodeAbortMessage(contactGroupId, privateGroupId,
							messageTimestamp);
			will(returnValue(message));
		}});
		expectSendMessage(ABORT, false);
	}

	private void expectSendMessage(MessageType type, boolean visible)
			throws Exception {
		BdfDictionary meta = BdfDictionary.of(new BdfEntry("me", "ta"));
		context.checking(new Expectations() {{
			oneOf(messageEncoder).encodeMetadata(type, privateGroupId,
					message.getTimestamp(), true, true, visible, false, false);
			will(returnValue(meta));
			oneOf(clientHelper).addLocalMessage(txn, message, meta, true,
					false);
		}});
	}

	void expectSetPrivateGroupVisibility(Visibility v) throws Exception {
		expectGetContactId();
		context.checking(new Expectations() {{
			oneOf(clientVersioningManager).getClientVisibility(txn, contactId,
					CLIENT_ID, MAJOR_VERSION);
			will(returnValue(SHARED));
			oneOf(db).setGroupVisibility(txn, contactId, privateGroupId, v);
		}});
	}

	void expectGetContactId() throws Exception {
		BdfDictionary groupMeta = BdfDictionary
				.of(new BdfEntry(GROUP_KEY_CONTACT_ID, contactId.getInt()));
		context.checking(new Expectations() {{
			oneOf(clientHelper)
					.getGroupMetadataAsDictionary(txn, contactGroupId);
			will(returnValue(groupMeta));
		}});
	}

	void expectIsSubscribedPrivateGroup() throws Exception {
		context.checking(new Expectations() {{
			oneOf(db).containsGroup(txn, privateGroupId);
			will(returnValue(true));
			oneOf(db).getGroup(txn, privateGroupId);
			will(returnValue(privateGroupGroup));
		}});
	}

	void expectIsNotSubscribedPrivateGroup() throws Exception {
		context.checking(new Expectations() {{
			oneOf(db).containsGroup(txn, privateGroupId);
			will(returnValue(false));
		}});
	}

	void expectMarkMessageVisibleInUi(MessageId m) throws Exception {
		BdfDictionary d = new BdfDictionary();
		context.checking(new Expectations() {{
			oneOf(messageEncoder).setVisibleInUi(d, true);
			oneOf(clientHelper).mergeMessageMetadata(txn, m, d);
		}});
	}

}

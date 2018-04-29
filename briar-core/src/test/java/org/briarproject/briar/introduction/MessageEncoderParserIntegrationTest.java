package org.briarproject.briar.introduction;

import org.briarproject.bramble.api.FormatException;
import org.briarproject.bramble.api.client.ClientHelper;
import org.briarproject.bramble.api.data.BdfDictionary;
import org.briarproject.bramble.api.data.MetadataEncoder;
import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.identity.AuthorFactory;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.properties.TransportProperties;
import org.briarproject.bramble.api.sync.Group;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.Message;
import org.briarproject.bramble.api.sync.MessageFactory;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.bramble.api.system.Clock;
import org.briarproject.bramble.test.BrambleTestCase;
import org.briarproject.briar.api.client.SessionId;
import org.junit.Test;

import java.util.Map;

import javax.inject.Inject;

import static org.briarproject.bramble.api.crypto.CryptoConstants.MAC_BYTES;
import static org.briarproject.bramble.api.crypto.CryptoConstants.MAX_SIGNATURE_BYTES;
import static org.briarproject.bramble.api.identity.AuthorConstants.MAX_PUBLIC_KEY_LENGTH;
import static org.briarproject.bramble.test.TestUtils.getGroup;
import static org.briarproject.bramble.test.TestUtils.getRandomBytes;
import static org.briarproject.bramble.test.TestUtils.getRandomId;
import static org.briarproject.bramble.test.TestUtils.getTransportPropertiesMap;
import static org.briarproject.bramble.util.StringUtils.getRandomString;
import static org.briarproject.briar.api.introduction.IntroductionConstants.MAX_REQUEST_MESSAGE_LENGTH;
import static org.briarproject.briar.api.introduction.IntroductionManager.CLIENT_ID;
import static org.briarproject.briar.api.introduction.IntroductionManager.MAJOR_VERSION;
import static org.briarproject.briar.introduction.MessageType.ABORT;
import static org.briarproject.briar.introduction.MessageType.REQUEST;
import static org.briarproject.briar.test.BriarTestUtils.getRealAuthor;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MessageEncoderParserIntegrationTest extends BrambleTestCase {

	@Inject
	ClientHelper clientHelper;
	@Inject
	MessageFactory messageFactory;
	@Inject
	MetadataEncoder metadataEncoder;
	@Inject
	AuthorFactory authorFactory;
	@Inject
	Clock clock;

	private final MessageEncoder messageEncoder;
	private final MessageParser messageParser;
	private final IntroductionValidator validator;

	private final Group group = getGroup(CLIENT_ID, MAJOR_VERSION);
	private final GroupId groupId = group.getId();
	private final long timestamp = 42L;
	private final SessionId sessionId = new SessionId(getRandomId());
	private final MessageId previousMsgId = new MessageId(getRandomId());
	private final Author author;
	private final String text = getRandomString(MAX_REQUEST_MESSAGE_LENGTH);
	private final byte[] ephemeralPublicKey =
			getRandomBytes(MAX_PUBLIC_KEY_LENGTH);
	private final byte[] mac = getRandomBytes(MAC_BYTES);
	private final byte[] signature = getRandomBytes(MAX_SIGNATURE_BYTES);

	public MessageEncoderParserIntegrationTest() {
		IntroductionIntegrationTestComponent component =
				DaggerIntroductionIntegrationTestComponent.builder().build();
		component.inject(this);

		messageEncoder = new MessageEncoderImpl(clientHelper, messageFactory);
		messageParser = new MessageParserImpl(clientHelper);
		validator = new IntroductionValidator(messageEncoder, clientHelper,
				metadataEncoder, clock);
		author = getRealAuthor(authorFactory);
	}

	@Test
	public void testRequestMessageMetadata() throws FormatException {
		BdfDictionary d = messageEncoder
				.encodeRequestMetadata(timestamp);
		MessageMetadata meta = messageParser.parseMetadata(d);

		assertEquals(REQUEST, meta.getMessageType());
		assertNull(meta.getSessionId());
		assertEquals(timestamp, meta.getTimestamp());
		assertFalse(meta.isLocal());
		assertFalse(meta.isRead());
		assertFalse(meta.isVisibleInConversation());
		assertFalse(meta.isAvailableToAnswer());
	}

	@Test
	public void testMessageMetadata() throws FormatException {
		BdfDictionary d = messageEncoder
				.encodeMetadata(ABORT, sessionId, timestamp, false, true,
						false);
		MessageMetadata meta = messageParser.parseMetadata(d);

		assertEquals(ABORT, meta.getMessageType());
		assertEquals(sessionId, meta.getSessionId());
		assertEquals(timestamp, meta.getTimestamp());
		assertFalse(meta.isLocal());
		assertTrue(meta.isRead());
		assertFalse(meta.isVisibleInConversation());
		assertFalse(meta.isAvailableToAnswer());
	}

	@Test
	public void testRequestMessage() throws FormatException {
		Message m = messageEncoder
				.encodeRequestMessage(groupId, timestamp, previousMsgId, author,
						text);
		validator.validateMessage(m, group, clientHelper.toList(m));
		RequestMessage rm =
				messageParser.parseRequestMessage(m, clientHelper.toList(m));

		assertEquals(m.getId(), rm.getMessageId());
		assertEquals(m.getGroupId(), rm.getGroupId());
		assertEquals(m.getTimestamp(), rm.getTimestamp());
		assertEquals(previousMsgId, rm.getPreviousMessageId());
		assertEquals(author, rm.getAuthor());
		assertEquals(text, rm.getMessage());
	}

	@Test
	public void testRequestMessageWithPreviousMsgNull() throws FormatException {
		Message m = messageEncoder
				.encodeRequestMessage(groupId, timestamp, null, author, text);
		validator.validateMessage(m, group, clientHelper.toList(m));
		RequestMessage rm =
				messageParser.parseRequestMessage(m, clientHelper.toList(m));

		assertNull(rm.getPreviousMessageId());
	}

	@Test
	public void testRequestMessageWithMsgNull() throws FormatException {
		Message m = messageEncoder
				.encodeRequestMessage(groupId, timestamp, previousMsgId, author,
						null);
		validator.validateMessage(m, group, clientHelper.toList(m));
		RequestMessage rm =
				messageParser.parseRequestMessage(m, clientHelper.toList(m));

		assertNull(rm.getMessage());
	}

	@Test
	public void testAcceptMessage() throws Exception {
		Map<TransportId, TransportProperties> transportProperties =
				getTransportPropertiesMap(2);

		long acceptTimestamp = 1337L;
		Message m = messageEncoder
				.encodeAcceptMessage(groupId, timestamp, previousMsgId,
						sessionId, ephemeralPublicKey, acceptTimestamp,
						transportProperties);
		validator.validateMessage(m, group, clientHelper.toList(m));
		AcceptMessage am =
				messageParser.parseAcceptMessage(m, clientHelper.toList(m));

		assertEquals(m.getId(), am.getMessageId());
		assertEquals(m.getGroupId(), am.getGroupId());
		assertEquals(m.getTimestamp(), am.getTimestamp());
		assertEquals(previousMsgId, am.getPreviousMessageId());
		assertEquals(sessionId, am.getSessionId());
		assertArrayEquals(ephemeralPublicKey, am.getEphemeralPublicKey());
		assertEquals(acceptTimestamp, am.getAcceptTimestamp());
		assertEquals(transportProperties, am.getTransportProperties());
	}

	@Test
	public void testDeclineMessage() throws Exception {
		Message m = messageEncoder
				.encodeDeclineMessage(groupId, timestamp, previousMsgId,
						sessionId);
		validator.validateMessage(m, group, clientHelper.toList(m));
		DeclineMessage dm =
				messageParser.parseDeclineMessage(m, clientHelper.toList(m));

		assertEquals(m.getId(), dm.getMessageId());
		assertEquals(m.getGroupId(), dm.getGroupId());
		assertEquals(m.getTimestamp(), dm.getTimestamp());
		assertEquals(previousMsgId, dm.getPreviousMessageId());
		assertEquals(sessionId, dm.getSessionId());
	}

	@Test
	public void testAuthMessage() throws Exception {
		Message m = messageEncoder
				.encodeAuthMessage(groupId, timestamp, previousMsgId,
						sessionId, mac, signature);
		validator.validateMessage(m, group, clientHelper.toList(m));
		AuthMessage am =
				messageParser.parseAuthMessage(m, clientHelper.toList(m));

		assertEquals(m.getId(), am.getMessageId());
		assertEquals(m.getGroupId(), am.getGroupId());
		assertEquals(m.getTimestamp(), am.getTimestamp());
		assertEquals(previousMsgId, am.getPreviousMessageId());
		assertEquals(sessionId, am.getSessionId());
		assertArrayEquals(mac, am.getMac());
		assertArrayEquals(signature, am.getSignature());
	}

	@Test
	public void testActivateMessage() throws Exception {
		Message m = messageEncoder
				.encodeActivateMessage(groupId, timestamp, previousMsgId,
						sessionId, mac);
		validator.validateMessage(m, group, clientHelper.toList(m));
		ActivateMessage am =
				messageParser.parseActivateMessage(m, clientHelper.toList(m));

		assertEquals(m.getId(), am.getMessageId());
		assertEquals(m.getGroupId(), am.getGroupId());
		assertEquals(m.getTimestamp(), am.getTimestamp());
		assertEquals(previousMsgId, am.getPreviousMessageId());
		assertEquals(sessionId, am.getSessionId());
		assertArrayEquals(mac, am.getMac());
	}

	@Test
	public void testAbortMessage() throws Exception {
		Message m = messageEncoder
				.encodeAbortMessage(groupId, timestamp, previousMsgId,
						sessionId);
		validator.validateMessage(m, group, clientHelper.toList(m));
		AbortMessage am =
				messageParser.parseAbortMessage(m, clientHelper.toList(m));

		assertEquals(m.getId(), am.getMessageId());
		assertEquals(m.getGroupId(), am.getGroupId());
		assertEquals(m.getTimestamp(), am.getTimestamp());
		assertEquals(previousMsgId, am.getPreviousMessageId());
		assertEquals(sessionId, am.getSessionId());
	}

}

package org.libreproject.bramble.transport.agreement;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.client.BdfMessageContext;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.sync.Group;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.bramble.test.BrambleMockTestCase;
import org.jmock.Expectations;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.libreproject.bramble.api.crypto.CryptoConstants.MAX_AGREEMENT_PUBLIC_KEY_BYTES;
import static org.libreproject.bramble.api.plugin.TransportId.MAX_TRANSPORT_ID_LENGTH;
import static org.libreproject.bramble.api.system.Clock.MIN_REASONABLE_TIME_MS;
import static org.libreproject.bramble.api.versioning.ClientVersioningManager.CLIENT_ID;
import static org.libreproject.bramble.api.versioning.ClientVersioningManager.MAJOR_VERSION;
import static org.libreproject.bramble.test.TestUtils.getGroup;
import static org.libreproject.bramble.test.TestUtils.getMessage;
import static org.libreproject.bramble.test.TestUtils.getRandomBytes;
import static org.libreproject.bramble.transport.agreement.MessageType.ACTIVATE;
import static org.libreproject.bramble.transport.agreement.MessageType.KEY;
import static org.libreproject.bramble.transport.agreement.TransportKeyAgreementConstants.MSG_KEY_PUBLIC_KEY;
import static org.libreproject.bramble.util.StringUtils.getRandomString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TransportKeyAgreementValidatorTest extends BrambleMockTestCase {

	private final ClientHelper clientHelper = context.mock(ClientHelper.class);
	private final MetadataEncoder metadataEncoder =
			context.mock(MetadataEncoder.class);
	private final Clock clock = context.mock(Clock.class);
	private final MessageEncoder messageEncoder =
			context.mock(MessageEncoder.class);
	private final TransportKeyAgreementValidator validator =
			new TransportKeyAgreementValidator(clientHelper, metadataEncoder,
					clock, messageEncoder);

	private final Group group = getGroup(CLIENT_ID, MAJOR_VERSION);
	private final Message message = getMessage(group.getId());

	@Test(expected = FormatException.class)
	public void testRejectsEmptyMessage() throws Exception {
		BdfList body = BdfList.of();
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsNullType() throws Exception {
		BdfList body = BdfList.of((Object) null);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonLongType() throws Exception {
		BdfList body = BdfList.of("123");
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsUnknownLongType() throws Exception {
		BdfList body = BdfList.of(ACTIVATE.getValue() + 1);
		validator.validateMessage(message, group, body);
	}

	@Test
	public void testAcceptsKeyMsg() throws Exception {
		TransportId transportId = new TransportId(getRandomString(1));
		context.checking(new Expectations() {{
			oneOf(messageEncoder)
					.encodeMessageMetadata(transportId, KEY, false);
			will(returnValue(new BdfDictionary()));
		}});

		byte[] publicKey = getRandomBytes(1);
		BdfList body =
				BdfList.of(KEY.getValue(), transportId.getString(), publicKey);
		BdfMessageContext msgCtx =
				validator.validateMessage(message, group, body);
		assertEquals(emptyList(), msgCtx.getDependencies());
		BdfDictionary d = msgCtx.getDictionary();
		assertArrayEquals(publicKey, d.getRaw(MSG_KEY_PUBLIC_KEY));
	}

	@Test
	public void testAcceptsKeyMsgMaxLengths() throws Exception {
		TransportId transportId =
				new TransportId(getRandomString(MAX_TRANSPORT_ID_LENGTH));
		context.checking(new Expectations() {{
			oneOf(messageEncoder)
					.encodeMessageMetadata(transportId, KEY, false);
			will(returnValue(new BdfDictionary()));
		}});

		byte[] publicKey = getRandomBytes(MAX_AGREEMENT_PUBLIC_KEY_BYTES);
		BdfList body =
				BdfList.of(KEY.getValue(), transportId.getString(), publicKey);
		BdfMessageContext msgCtx =
				validator.validateMessage(message, group, body);
		assertEquals(emptyList(), msgCtx.getDependencies());
		BdfDictionary d = msgCtx.getDictionary();
		assertArrayEquals(publicKey, d.getRaw(MSG_KEY_PUBLIC_KEY));
	}

	@Test
	public void testAcceptsMinTimestampKeyMsg() throws Exception {
		Message message =
				getMessage(group.getId(), 1234, MIN_REASONABLE_TIME_MS);
		TransportId transportId = new TransportId(getRandomString(1));
		context.checking(new Expectations() {{
			oneOf(messageEncoder)
					.encodeMessageMetadata(transportId, KEY, false);
			will(returnValue(new BdfDictionary()));
		}});

		byte[] publicKey = getRandomBytes(1);
		BdfList body =
				BdfList.of(KEY.getValue(), transportId.getString(), publicKey);
		BdfMessageContext msgCtx =
				validator.validateMessage(message, group, body);
		assertEquals(emptyList(), msgCtx.getDependencies());
		BdfDictionary d = msgCtx.getDictionary();
		assertArrayEquals(publicKey, d.getRaw(MSG_KEY_PUBLIC_KEY));
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongKeyMsg() throws Exception {
		BdfList body = BdfList.of(KEY.getValue(), getRandomString(1),
				getRandomBytes(1), 1);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooShortKeyMsg() throws Exception {
		BdfList body = BdfList.of(KEY.getValue(), getRandomString(1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsEmptyTransportIdKeyMsg() throws Exception {
		BdfList body = BdfList.of(KEY.getValue(), "", getRandomBytes(1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongTransportIdKeyMsg() throws Exception {
		BdfList body = BdfList.of(KEY.getValue(),
				getRandomString(MAX_TRANSPORT_ID_LENGTH + 1),
				getRandomBytes(1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonStringTransportIdKeyMsg() throws Exception {
		BdfList body = BdfList.of(KEY.getValue(),
				getRandomBytes(MAX_TRANSPORT_ID_LENGTH),
				getRandomBytes(1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsEmptyPublicKeyKeyMsg() throws Exception {
		BdfList body = BdfList.of(KEY.getValue(),
				getRandomString(1),
				getRandomBytes(0));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongPublicKeyKeyMsg() throws Exception {
		BdfList body = BdfList.of(KEY.getValue(),
				getRandomString(1),
				getRandomBytes(MAX_AGREEMENT_PUBLIC_KEY_BYTES + 1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonBytesPublicKeyKeyMsg() throws Exception {
		BdfList body = BdfList.of(KEY.getValue(),
				getRandomString(1),
				getRandomString(MAX_AGREEMENT_PUBLIC_KEY_BYTES));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooOldTimestampKeyMsg() throws Exception {
		Message message =
				getMessage(group.getId(), 1234, MIN_REASONABLE_TIME_MS - 1);
		BdfList body = BdfList.of(KEY.getValue(), getRandomString(1),
				getRandomBytes(1));
		validator.validateMessage(message, group, body);
	}

	@Test
	public void testAcceptsActivateMsg() throws Exception {
		TransportId transportId = new TransportId(getRandomString(1));
		BdfDictionary meta = new BdfDictionary();
		context.checking(new Expectations() {{
			oneOf(messageEncoder)
					.encodeMessageMetadata(transportId, ACTIVATE, false);
			will(returnValue(meta));
		}});

		MessageId msgId = new MessageId(getRandomBytes(MessageId.LENGTH));
		BdfList body = BdfList.of(ACTIVATE.getValue(), transportId.getString(),
				msgId.getBytes());

		BdfMessageContext msgCtx =
				validator.validateMessage(message, group, body);
		assertEquals(singletonList(msgId), msgCtx.getDependencies());
		assertEquals(meta, msgCtx.getDictionary());
	}

	@Test
	public void testAcceptsActivateMsgMaxTransportIdLength() throws Exception {
		TransportId transportId =
				new TransportId(getRandomString(MAX_TRANSPORT_ID_LENGTH));
		BdfDictionary meta = new BdfDictionary();
		context.checking(new Expectations() {{
			oneOf(messageEncoder)
					.encodeMessageMetadata(transportId, ACTIVATE, false);
			will(returnValue(meta));
		}});

		MessageId msgId = new MessageId(getRandomBytes(MessageId.LENGTH));
		BdfList body = BdfList.of(ACTIVATE.getValue(), transportId.getString(),
				msgId.getBytes());

		BdfMessageContext msgCtx =
				validator.validateMessage(message, group, body);
		assertEquals(singletonList(msgId), msgCtx.getDependencies());
		assertEquals(meta, msgCtx.getDictionary());
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongActivateMsg() throws Exception {
		BdfList body = BdfList.of(ACTIVATE.getValue(), getRandomString(1),
				getRandomBytes(MessageId.LENGTH), 1);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooShortActivateMsg() throws Exception {
		BdfList body = BdfList.of(ACTIVATE.getValue(), getRandomString(1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsEmptyTransportIdActivateMsg() throws Exception {
		BdfList body = BdfList.of(ACTIVATE.getValue(), "",
				getRandomBytes(MessageId.LENGTH));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonStringTransportIdActivateMsg() throws Exception {
		BdfList body = BdfList.of(ACTIVATE.getValue(), 123,
				getRandomBytes(MessageId.LENGTH));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongTransportIdActivateMsg() throws Exception {
		BdfList body = BdfList.of(ACTIVATE.getValue(),
				getRandomString(MAX_TRANSPORT_ID_LENGTH + 1),
				getRandomBytes(MessageId.LENGTH));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooShortMsgIdActivateMsg() throws Exception {
		BdfList body = BdfList.of(ACTIVATE.getValue(),
				getRandomString(1),
				getRandomBytes(MessageId.LENGTH - 1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongMsgIdActivateMsg() throws Exception {
		BdfList body = BdfList.of(ACTIVATE.getValue(),
				getRandomString(1),
				getRandomBytes(MessageId.LENGTH + 1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonByteMsgIdActivateMsg() throws Exception {
		BdfList body = BdfList.of(ACTIVATE.getValue(),
				getRandomString(1),
				getRandomString(MessageId.LENGTH));
		validator.validateMessage(message, group, body);
	}
}

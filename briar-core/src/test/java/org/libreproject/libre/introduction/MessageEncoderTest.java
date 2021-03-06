package org.libreproject.libre.introduction;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageFactory;
import org.libreproject.bramble.test.BrambleMockTestCase;
import org.jmock.Expectations;
import org.junit.Test;

import static org.libreproject.bramble.api.sync.SyncConstants.MAX_MESSAGE_BODY_LENGTH;
import static org.libreproject.bramble.test.TestUtils.getAuthor;
import static org.libreproject.bramble.test.TestUtils.getMessage;
import static org.libreproject.bramble.test.TestUtils.getRandomId;
import static org.libreproject.bramble.util.StringUtils.getRandomString;
import static org.libreproject.libre.api.introduction.IntroductionConstants.MAX_INTRODUCTION_TEXT_LENGTH;
import static org.libreproject.libre.introduction.MessageType.REQUEST;

public class MessageEncoderTest extends BrambleMockTestCase {

	private final ClientHelper clientHelper = context.mock(ClientHelper.class);
	private final MessageFactory messageFactory =
			context.mock(MessageFactory.class);
	private final MessageEncoder messageEncoder =
			new MessageEncoderImpl(clientHelper, messageFactory);

	private final GroupId groupId = new GroupId(getRandomId());
	private final Message message =
			getMessage(groupId, MAX_MESSAGE_BODY_LENGTH);
	private final long timestamp = message.getTimestamp();
	private final byte[] body = message.getBody();
	private final Author author = getAuthor();
	private final BdfList authorList = new BdfList();
	private final String text = getRandomString(MAX_INTRODUCTION_TEXT_LENGTH);

	@Test
	public void testEncodeRequestMessage() throws FormatException {
		context.checking(new Expectations() {{
			oneOf(clientHelper).toList(author);
			will(returnValue(authorList));
		}});
		expectCreateMessage(
				BdfList.of(REQUEST.getValue(), null, authorList, text));

		messageEncoder.encodeRequestMessage(groupId, timestamp, null,
				author, text);
	}

	private void expectCreateMessage(BdfList bodyList) throws FormatException {
		context.checking(new Expectations() {{
			oneOf(clientHelper).toByteArray(bodyList);
			will(returnValue(body));
			oneOf(messageFactory).createMessage(groupId, timestamp, body,
					Message.MessageType.DEFAULT);
			will(returnValue(message));
		}});
	}

}

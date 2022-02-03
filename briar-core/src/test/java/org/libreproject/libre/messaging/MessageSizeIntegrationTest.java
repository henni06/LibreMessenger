package org.libreproject.libre.messaging;

import org.libreproject.bramble.api.UniqueId;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.crypto.CryptoComponent;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.identity.AuthorFactory;
import org.libreproject.bramble.api.identity.LocalAuthor;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageFactory;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.test.BrambleTestCase;
import org.libreproject.libre.api.attachment.AttachmentHeader;
import org.libreproject.libre.api.forum.ForumPost;
import org.libreproject.libre.api.forum.ForumPostFactory;
import org.libreproject.libre.api.messaging.PrivateMessage;
import org.libreproject.libre.api.messaging.PrivateMessageFactory;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static org.libreproject.bramble.api.identity.AuthorConstants.MAX_AUTHOR_NAME_LENGTH;
import static org.libreproject.bramble.api.identity.AuthorConstants.MAX_PUBLIC_KEY_LENGTH;
import static org.libreproject.bramble.api.record.Record.MAX_RECORD_PAYLOAD_BYTES;
import static org.libreproject.bramble.test.TestUtils.getRandomBytes;
import static org.libreproject.bramble.test.TestUtils.getRandomId;
import static org.libreproject.bramble.util.IoUtils.copyAndClose;
import static org.libreproject.bramble.util.StringUtils.getRandomString;
import static org.libreproject.libre.api.attachment.MediaConstants.MAX_CONTENT_TYPE_BYTES;
import static org.libreproject.libre.api.attachment.MediaConstants.MAX_IMAGE_SIZE;
import static org.libreproject.libre.api.autodelete.AutoDeleteConstants.MAX_AUTO_DELETE_TIMER_MS;
import static org.libreproject.libre.api.forum.ForumConstants.MAX_FORUM_POST_TEXT_LENGTH;
import static org.libreproject.libre.api.messaging.MessagingConstants.MAX_ATTACHMENTS_PER_MESSAGE;
import static org.libreproject.libre.api.messaging.MessagingConstants.MAX_PRIVATE_MESSAGE_TEXT_LENGTH;
import static org.libreproject.libre.messaging.MessageTypes.ATTACHMENT;
import static org.junit.Assert.assertTrue;

public class MessageSizeIntegrationTest extends BrambleTestCase {

	@Inject
	CryptoComponent crypto;
	@Inject
	AuthorFactory authorFactory;
	@Inject
	PrivateMessageFactory privateMessageFactory;
	@Inject
	ForumPostFactory forumPostFactory;
	@Inject
	ClientHelper clientHelper;
	@Inject
	MessageFactory messageFactory;

	public MessageSizeIntegrationTest() {
		MessageSizeIntegrationTestComponent component =
				DaggerMessageSizeIntegrationTestComponent.builder().build();
		MessageSizeIntegrationTestComponent.Helper
				.injectEagerSingletons(component);
		component.inject(this);
	}

	@Test
	public void testLegacyPrivateMessageFitsIntoRecord() throws Exception {
		// Create a maximum-length private message
		GroupId groupId = new GroupId(getRandomId());
		long timestamp = Long.MAX_VALUE;
		String text = getRandomString(MAX_PRIVATE_MESSAGE_TEXT_LENGTH);
		PrivateMessage message = privateMessageFactory
				.createLegacyPrivateMessage(groupId, timestamp, text);
		// Check the size of the serialised message
		int length = message.getMessage().getRawLength();
		assertTrue(length > UniqueId.LENGTH + 8
				+ MAX_PRIVATE_MESSAGE_TEXT_LENGTH);
		assertTrue(length <= MAX_RECORD_PAYLOAD_BYTES);
	}

	@Test
	public void testPrivateMessageFitsIntoRecord() throws Exception {
		// Create a maximum-length private message
		GroupId groupId = new GroupId(getRandomId());
		long timestamp = Long.MAX_VALUE;
		String text = getRandomString(MAX_PRIVATE_MESSAGE_TEXT_LENGTH);
		// Create the maximum number of maximum-length attachment headers
		List<AttachmentHeader> headers = new ArrayList<>();
		for (int i = 0; i < MAX_ATTACHMENTS_PER_MESSAGE; i++) {
			headers.add(new AttachmentHeader(groupId,
					new MessageId(getRandomId()),
					getRandomString(MAX_CONTENT_TYPE_BYTES)));
		}
		PrivateMessage message = privateMessageFactory.createPrivateMessage(
				groupId, timestamp, text, headers, MAX_AUTO_DELETE_TIMER_MS);
		// Check the size of the serialised message
		int length = message.getMessage().getRawLength();
		assertTrue(length > UniqueId.LENGTH + 8
				+ MAX_PRIVATE_MESSAGE_TEXT_LENGTH + MAX_ATTACHMENTS_PER_MESSAGE
				* (UniqueId.LENGTH + MAX_CONTENT_TYPE_BYTES) + 4);
		assertTrue(length <= MAX_RECORD_PAYLOAD_BYTES);
	}

	@Test
	public void testAttachmentFitsIntoRecord() throws Exception {
		// Create a maximum-length attachment
		String contentType = getRandomString(MAX_CONTENT_TYPE_BYTES);
		byte[] data = getRandomBytes(MAX_IMAGE_SIZE);

		ByteArrayInputStream dataIn = new ByteArrayInputStream(data);
		ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();
		byte[] descriptor =
				clientHelper.toByteArray(BdfList.of(ATTACHMENT, contentType));
		bodyOut.write(descriptor);
		copyAndClose(dataIn, bodyOut);
		byte[] body = bodyOut.toByteArray();

		GroupId groupId = new GroupId(getRandomId());
		long timestamp = Long.MAX_VALUE;
		Message message =
				messageFactory.createMessage(groupId, timestamp, body,
						Message.MessageType.DEFAULT);

		// Check the size of the serialised message
		int length = message.getRawLength();
		assertTrue(length > UniqueId.LENGTH + 8
				+ 1 + MAX_CONTENT_TYPE_BYTES + MAX_IMAGE_SIZE);
		assertTrue(length <= MAX_RECORD_PAYLOAD_BYTES);
	}

	@Test
	public void testForumPostFitsIntoRecord() throws Exception {
		// Create a maximum-length author
		String authorName = getRandomString(MAX_AUTHOR_NAME_LENGTH);
		LocalAuthor author = authorFactory.createLocalAuthor(authorName);
		// Create a maximum-length forum post
		GroupId groupId = new GroupId(getRandomId());
		long timestamp = Long.MAX_VALUE;
		MessageId parent = new MessageId(getRandomId());
		String text = getRandomString(MAX_FORUM_POST_TEXT_LENGTH);
		ForumPost post = forumPostFactory.createPost(groupId,
				timestamp, parent, author, text);
		// Check the size of the serialised message
		int length = post.getMessage().getRawLength();
		assertTrue(length > UniqueId.LENGTH + 8 + UniqueId.LENGTH + 4
				+ MAX_AUTHOR_NAME_LENGTH + MAX_PUBLIC_KEY_LENGTH
				+ MAX_FORUM_POST_TEXT_LENGTH);
		assertTrue(length <= MAX_RECORD_PAYLOAD_BYTES);
	}
}

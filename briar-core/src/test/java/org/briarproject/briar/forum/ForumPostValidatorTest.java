package org.briarproject.briar.forum;

import org.briarproject.bramble.api.FormatException;
import org.briarproject.bramble.api.UniqueId;
import org.briarproject.bramble.api.client.BdfMessageContext;
import org.briarproject.bramble.api.data.BdfDictionary;
import org.briarproject.bramble.api.data.BdfList;
import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.sync.InvalidMessageException;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.bramble.test.ValidatorTestCase;
import org.jmock.Expectations;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.Collection;

import static org.briarproject.bramble.api.identity.AuthorConstants.MAX_SIGNATURE_LENGTH;
import static org.briarproject.bramble.test.TestUtils.getAuthor;
import static org.briarproject.bramble.test.TestUtils.getRandomBytes;
import static org.briarproject.bramble.test.TestUtils.getRandomId;
import static org.briarproject.bramble.util.StringUtils.getRandomString;
import static org.briarproject.briar.api.blog.BlogConstants.KEY_READ;
import static org.briarproject.briar.api.forum.ForumConstants.KEY_AUTHOR;
import static org.briarproject.briar.api.forum.ForumConstants.KEY_PARENT;
import static org.briarproject.briar.api.forum.ForumConstants.KEY_TIMESTAMP;
import static org.briarproject.briar.api.forum.ForumConstants.MAX_FORUM_POST_BODY_LENGTH;
import static org.briarproject.briar.api.forum.ForumPostFactory.SIGNING_LABEL_POST;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ForumPostValidatorTest extends ValidatorTestCase {

	private final MessageId parentId = new MessageId(getRandomId());
	private final String content = getRandomString(MAX_FORUM_POST_BODY_LENGTH);
	private final byte[] signature = getRandomBytes(MAX_SIGNATURE_LENGTH);
	private final Author author = getAuthor();
	private final String authorName = author.getName();
	private final byte[] authorPublicKey = author.getPublicKey();
	private final BdfList authorList = BdfList.of(author.getFormatVersion(),
			authorName, authorPublicKey);
	private final BdfList signedWithParent = BdfList.of(groupId, timestamp,
			parentId.getBytes(), authorList, content);
	private final BdfList signedWithoutParent = BdfList.of(groupId, timestamp,
			null, authorList, content);

	private final ForumPostValidator v = new ForumPostValidator(clientHelper,
			metadataEncoder, clock);

	@Test(expected = FormatException.class)
	public void testRejectsTooShortBody() throws Exception {
		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, content));
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongBody() throws Exception {
		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, content, signature, 123));
	}

	@Test
	public void testAcceptsNullParentId() throws Exception {
		expectCreateAuthor();
		context.checking(new Expectations() {{
			oneOf(clientHelper).verifySignature(signature, SIGNING_LABEL_POST,
					signedWithoutParent, authorPublicKey);
		}});

		BdfMessageContext messageContext = v.validateMessage(message, group,
				BdfList.of(null, authorList, content, signature));
		assertExpectedContext(messageContext, false);
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonRawParentId() throws Exception {
		v.validateMessage(message, group,
				BdfList.of(123, authorList, content, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooShortParentId() throws Exception {
		byte[] invalidParentId = getRandomBytes(UniqueId.LENGTH - 1);
		v.validateMessage(message, group,
				BdfList.of(invalidParentId, authorList, content, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongParentId() throws Exception {
		byte[] invalidParentId = getRandomBytes(UniqueId.LENGTH + 1);
		v.validateMessage(message, group,
				BdfList.of(invalidParentId, authorList, content, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNullAuthorList() throws Exception {
		v.validateMessage(message, group,
				BdfList.of(parentId, null, content, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonListAuthorList() throws Exception {
		v.validateMessage(message, group,
				BdfList.of(parentId, 123, content, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsInvalidAuthor() throws Exception {
		context.checking(new Expectations() {{
			oneOf(clientHelper).parseAndValidateAuthor(authorList);
			will(throwException(new FormatException()));
		}});
		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, content, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNullContent() throws Exception {
		expectCreateAuthor();

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, null, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonStringContent() throws Exception {
		expectCreateAuthor();

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, 123, signature));
	}

	@Test
	public void testAcceptsMinLengthContent() throws Exception {
		String shortContent = "";
		BdfList signedWithShortContent = BdfList.of(groupId, timestamp,
				parentId.getBytes(), authorList, shortContent);

		expectCreateAuthor();
		context.checking(new Expectations() {{
			oneOf(clientHelper).verifySignature(signature, SIGNING_LABEL_POST,
					signedWithShortContent, authorPublicKey);
		}});

		BdfMessageContext messageContext = v.validateMessage(message, group,
				BdfList.of(parentId, authorList, shortContent, signature));
		assertExpectedContext(messageContext, true);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongContent() throws Exception {
		String invalidContent = getRandomString(MAX_FORUM_POST_BODY_LENGTH + 1);

		expectCreateAuthor();

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, invalidContent, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNullSignature() throws Exception {
		expectCreateAuthor();

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, content, null));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonRawSignature() throws Exception {
		expectCreateAuthor();

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, content, 123));
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongSignature() throws Exception {
		byte[] invalidSignature = getRandomBytes(MAX_SIGNATURE_LENGTH + 1);

		expectCreateAuthor();

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, content, invalidSignature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsIfVerifyingSignatureThrowsFormatException()
			throws Exception {
		expectCreateAuthor();
		context.checking(new Expectations() {{
			oneOf(clientHelper).verifySignature(signature, SIGNING_LABEL_POST,
					signedWithParent, authorPublicKey);
			will(throwException(new FormatException()));
		}});

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, content, signature));
	}

	@Test(expected = InvalidMessageException.class)
	public void testRejectsIfVerifyingSignatureThrowsGeneralSecurityException()
			throws Exception {
		expectCreateAuthor();
		context.checking(new Expectations() {{
			oneOf(clientHelper).verifySignature(signature, SIGNING_LABEL_POST,
					signedWithParent, authorPublicKey);
			will(throwException(new GeneralSecurityException()));
		}});

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, content, signature));
	}

	private void expectCreateAuthor() throws Exception {
		context.checking(new Expectations() {{
			oneOf(clientHelper).parseAndValidateAuthor(authorList);
			will(returnValue(author));
		}});
	}

	private void assertExpectedContext(BdfMessageContext messageContext,
			boolean hasParent) throws FormatException {
		BdfDictionary meta = messageContext.getDictionary();
		Collection<MessageId> dependencies = messageContext.getDependencies();
		if (hasParent) {
			assertEquals(4, meta.size());
			assertArrayEquals(parentId.getBytes(), meta.getRaw(KEY_PARENT));
			assertEquals(1, dependencies.size());
			assertEquals(parentId, dependencies.iterator().next());
		} else {
			assertEquals(3, meta.size());
			assertEquals(0, dependencies.size());
		}
		assertEquals(timestamp, meta.getLong(KEY_TIMESTAMP).longValue());
		assertFalse(meta.getBoolean(KEY_READ));
		assertEquals(authorList, meta.getList(KEY_AUTHOR));
	}
}

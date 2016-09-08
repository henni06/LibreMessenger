package org.briarproject.blogs;

import org.briarproject.api.FormatException;
import org.briarproject.api.blogs.Blog;
import org.briarproject.api.blogs.BlogFactory;
import org.briarproject.api.blogs.MessageType;
import org.briarproject.api.clients.BdfMessageContext;
import org.briarproject.api.clients.ClientHelper;
import org.briarproject.api.crypto.CryptoComponent;
import org.briarproject.api.crypto.KeyParser;
import org.briarproject.api.crypto.PublicKey;
import org.briarproject.api.crypto.Signature;
import org.briarproject.api.data.BdfDictionary;
import org.briarproject.api.data.BdfEntry;
import org.briarproject.api.data.BdfList;
import org.briarproject.api.data.MetadataEncoder;
import org.briarproject.api.identity.Author;
import org.briarproject.api.sync.Group;
import org.briarproject.api.sync.GroupFactory;
import org.briarproject.api.sync.InvalidMessageException;
import org.briarproject.api.sync.Message;
import org.briarproject.api.sync.MessageFactory;
import org.briarproject.api.sync.MessageId;
import org.briarproject.api.system.Clock;
import org.briarproject.clients.BdfMessageValidator;

import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Collections;

import static org.briarproject.api.blogs.BlogConstants.KEY_AUTHOR;
import static org.briarproject.api.blogs.BlogConstants.KEY_AUTHOR_ID;
import static org.briarproject.api.blogs.BlogConstants.KEY_AUTHOR_NAME;
import static org.briarproject.api.blogs.BlogConstants.KEY_COMMENT;
import static org.briarproject.api.blogs.BlogConstants.KEY_ORIGINAL_MSG_ID;
import static org.briarproject.api.blogs.BlogConstants.KEY_ORIGINAL_PARENT_MSG_ID;
import static org.briarproject.api.blogs.BlogConstants.KEY_PARENT_MSG_ID;
import static org.briarproject.api.blogs.BlogConstants.KEY_PUBLIC_KEY;
import static org.briarproject.api.blogs.BlogConstants.KEY_READ;
import static org.briarproject.api.blogs.BlogConstants.KEY_TIMESTAMP;
import static org.briarproject.api.blogs.BlogConstants.KEY_TIME_RECEIVED;
import static org.briarproject.api.blogs.BlogConstants.KEY_TYPE;
import static org.briarproject.api.blogs.BlogConstants.MAX_BLOG_POST_BODY_LENGTH;
import static org.briarproject.api.blogs.MessageType.COMMENT;
import static org.briarproject.api.blogs.MessageType.POST;
import static org.briarproject.api.identity.AuthorConstants.MAX_SIGNATURE_LENGTH;

class BlogPostValidator extends BdfMessageValidator {

	private final CryptoComponent crypto;
	private final GroupFactory groupFactory;
	private final MessageFactory messageFactory;
	private final BlogFactory blogFactory;

	BlogPostValidator(CryptoComponent crypto, GroupFactory groupFactory,
			MessageFactory messageFactory, BlogFactory blogFactory,
			ClientHelper clientHelper, MetadataEncoder metadataEncoder,
			Clock clock) {
		super(clientHelper, metadataEncoder, clock);

		this.crypto = crypto;
		this.groupFactory = groupFactory;
		this.messageFactory = messageFactory;
		this.blogFactory = blogFactory;
	}

	@Override
	protected BdfMessageContext validateMessage(Message m, Group g,
			BdfList body) throws InvalidMessageException, FormatException {

		BdfMessageContext c;

		int type = body.getLong(0).intValue();
		body.removeElementAt(0);
		switch (MessageType.valueOf(type)) {
			case POST:
				c = validatePost(m, g, body);
				addMessageMetadata(c, m.getTimestamp());
				break;
			case COMMENT:
				c = validateComment(m, g, body);
				addMessageMetadata(c, m.getTimestamp());
				break;
			case WRAPPED_POST:
				c = validateWrappedPost(m, g, body);
				break;
			case WRAPPED_COMMENT:
				c = validateWrappedComment(m, g, body);
				break;
			default:
				throw new InvalidMessageException("Unknown Message Type");
		}
		c.getDictionary().put(KEY_TYPE, type);
		return c;
	}

	private BdfMessageContext validatePost(Message m, Group g, BdfList body)
			throws InvalidMessageException, FormatException {

		// Content, Signature
		checkSize(body, 2);
		String postBody = body.getString(0);
		checkLength(postBody, 0, MAX_BLOG_POST_BODY_LENGTH);

		// Verify Signature
		byte[] sig = body.getRaw(1);
		checkLength(sig, 1, MAX_SIGNATURE_LENGTH);
		BdfList signed = BdfList.of(g.getId(), m.getTimestamp(), postBody);
		Blog b = blogFactory.parseBlog(g, ""); // description doesn't matter
		Author a = b.getAuthor();
		verifySignature(sig, a.getPublicKey(), signed);

		// Return the metadata and dependencies
		BdfDictionary meta = new BdfDictionary();
		meta.put(KEY_ORIGINAL_MSG_ID, m.getId());
		meta.put(KEY_AUTHOR, authorToBdfDictionary(a));
		return new BdfMessageContext(meta);
	}

	private BdfMessageContext validateComment(Message m, Group g, BdfList body)
			throws InvalidMessageException, FormatException {

		// comment, parent_original_id, parent_id, signature
		checkSize(body, 4);

		// Comment
		String comment = body.getOptionalString(0);
		checkLength(comment, 1, MAX_BLOG_POST_BODY_LENGTH);

		// parent_original_id
		// The ID of a post or comment in this group or another group
		byte[] pOriginalIdBytes = body.getRaw(1);
		checkLength(pOriginalIdBytes, MessageId.LENGTH);
		MessageId pOriginalId = new MessageId(pOriginalIdBytes);

		// parent_id
		// The ID of a post, comment, wrapped post or wrapped comment in this
		// group, which had the ID parent_original_id in the group
		// where it was originally posted
		byte[] currentIdBytes = body.getRaw(2);
		checkLength(currentIdBytes, MessageId.LENGTH);
		MessageId currentId = new MessageId(currentIdBytes);

		// Signature
		byte[] sig = body.getRaw(3);
		checkLength(sig, 0, MAX_SIGNATURE_LENGTH);
		BdfList signed =
				BdfList.of(g.getId(), m.getTimestamp(), comment, pOriginalId,
						currentId);
		Blog b = blogFactory.parseBlog(g, ""); // description doesn't matter
		Author a = b.getAuthor();
		verifySignature(sig, a.getPublicKey(), signed);

		// Return the metadata and dependencies
		BdfDictionary meta = new BdfDictionary();
		if (comment != null) meta.put(KEY_COMMENT, comment);
		meta.put(KEY_ORIGINAL_MSG_ID, m.getId());
		meta.put(KEY_ORIGINAL_PARENT_MSG_ID, pOriginalId);
		meta.put(KEY_PARENT_MSG_ID, currentId);
		meta.put(KEY_AUTHOR, authorToBdfDictionary(a));
		Collection<MessageId> dependencies = Collections.singleton(currentId);
		return new BdfMessageContext(meta, dependencies);
	}

	private BdfMessageContext validateWrappedPost(Message m, Group g,
			BdfList body) throws InvalidMessageException, FormatException {

		// p_group descriptor, p_timestamp, p_content, p_signature
		checkSize(body, 4);

		// Group Descriptor
		byte[] descriptor = body.getRaw(0);

		// Timestamp of Wrapped Post
		long wTimestamp = body.getLong(1);
		if (wTimestamp < 0) throw new FormatException();

		// Content of Wrapped Post
		String content = body.getString(2);

		// Signature of Wrapped Post
		byte[] signature = body.getRaw(3);
		checkLength(signature, 1, MAX_SIGNATURE_LENGTH);

		// Get and Validate the Wrapped Message
		Group wGroup = groupFactory
				.createGroup(BlogManagerImpl.CLIENT_ID, descriptor);
		BdfList wBodyList = BdfList.of(POST.getInt(), content, signature);
		byte[] wBody = clientHelper.toByteArray(wBodyList);
		Message wMessage =
				messageFactory.createMessage(wGroup.getId(), wTimestamp, wBody);
		wBodyList.remove(0);
		BdfMessageContext c = validatePost(wMessage, wGroup, wBodyList);

		// Return the metadata and dependencies
		BdfDictionary meta = new BdfDictionary();
		meta.put(KEY_ORIGINAL_MSG_ID, wMessage.getId());
		meta.put(KEY_TIMESTAMP, wTimestamp);
		meta.put(KEY_AUTHOR, c.getDictionary().getDictionary(KEY_AUTHOR));
		return new BdfMessageContext(meta);
	}

	private BdfMessageContext validateWrappedComment(Message m, Group g,
			BdfList body) throws InvalidMessageException, FormatException {

		// c_group descriptor, c_timestamp, c_comment, c_parent_original_id,
		// c_parent_id, c_signature, parent_id
		checkSize(body, 7);

		// Group Descriptor
		byte[] descriptor = body.getRaw(0);

		// Timestamp of Wrapped Comment
		long wTimestamp = body.getLong(1);
		if (wTimestamp < 0) throw new FormatException();

		// Body of Wrapped Comment
		String comment = body.getOptionalString(2);
		checkLength(comment, 1, MAX_BLOG_POST_BODY_LENGTH);

		// c_parent_original_id
		// Taken from the original comment
		byte[] pOriginalIdBytes = body.getRaw(3);
		checkLength(pOriginalIdBytes, MessageId.LENGTH);
		MessageId pOriginalId = new MessageId(pOriginalIdBytes);

		// c_parent_id
		// Taken from the original comment
		byte[] oldIdBytes = body.getRaw(4);
		checkLength(oldIdBytes, MessageId.LENGTH);
		MessageId oldId = new MessageId(oldIdBytes);

		// c_signature
		// Taken from the original comment
		byte[] signature = body.getRaw(5);
		checkLength(signature, 1, MAX_SIGNATURE_LENGTH);

		// parent_id
		// The ID of a post, comment, wrapped post or wrapped comment in this
		// group, which had the ID c_parent_original_id in the group
		// where it was originally posted
		byte[] parentIdBytes = body.getRaw(6);
		checkLength(parentIdBytes, MessageId.LENGTH);
		MessageId parentId = new MessageId(parentIdBytes);

		// Get and Validate the Wrapped Comment
		Group wGroup = groupFactory
				.createGroup(BlogManagerImpl.CLIENT_ID, descriptor);
		BdfList wBodyList =	BdfList.of(COMMENT.getInt(), comment, pOriginalId,
				oldId, signature);
		byte[] wBody = clientHelper.toByteArray(wBodyList);
		Message wMessage =
				messageFactory.createMessage(wGroup.getId(), wTimestamp, wBody);
		wBodyList.remove(0);
		BdfMessageContext c = validateComment(wMessage, wGroup, wBodyList);

		// Return the metadata and dependencies
		Collection<MessageId> dependencies = Collections.singleton(parentId);
		BdfDictionary meta = new BdfDictionary();
		meta.put(KEY_ORIGINAL_MSG_ID, wMessage.getId());
		meta.put(KEY_ORIGINAL_PARENT_MSG_ID, pOriginalId);
		meta.put(KEY_PARENT_MSG_ID, parentId);
		meta.put(KEY_TIMESTAMP, wTimestamp);
		if (comment != null) meta.put(KEY_COMMENT, comment);
		meta.put(KEY_AUTHOR, c.getDictionary().getDictionary(KEY_AUTHOR));
		return new BdfMessageContext(meta, dependencies);
	}

	private void verifySignature(byte[] sig, byte[] publicKey, BdfList signed)
			throws InvalidMessageException {
		try {
			// Parse the public key
			KeyParser keyParser = crypto.getSignatureKeyParser();
			PublicKey key = keyParser.parsePublicKey(publicKey);
			// Verify the signature
			Signature signature = crypto.getSignature();
			signature.initVerify(key);
			signature.update(clientHelper.toByteArray(signed));
			if (!signature.verify(sig)) {
				throw new InvalidMessageException("Invalid signature");
			}
		} catch (GeneralSecurityException e) {
			throw new InvalidMessageException("Invalid public key");
		} catch (FormatException e) {
			throw new InvalidMessageException(e);
		}
	}

	static BdfDictionary authorToBdfDictionary(Author a) {
		return BdfDictionary.of(
				new BdfEntry(KEY_AUTHOR_ID, a.getId()),
				new BdfEntry(KEY_AUTHOR_NAME, a.getName()),
				new BdfEntry(KEY_PUBLIC_KEY, a.getPublicKey())
		);
	}

	private void addMessageMetadata(BdfMessageContext c, long time) {
		c.getDictionary().put(KEY_TIMESTAMP, time);
		c.getDictionary().put(KEY_TIME_RECEIVED, clock.currentTimeMillis());
		c.getDictionary().put(KEY_READ, false);
	}

}

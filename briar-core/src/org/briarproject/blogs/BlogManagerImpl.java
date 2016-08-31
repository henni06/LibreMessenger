package org.briarproject.blogs;

import org.briarproject.api.FormatException;
import org.briarproject.api.blogs.Blog;
import org.briarproject.api.blogs.BlogCommentHeader;
import org.briarproject.api.blogs.BlogFactory;
import org.briarproject.api.blogs.BlogManager;
import org.briarproject.api.blogs.BlogPost;
import org.briarproject.api.blogs.BlogPostFactory;
import org.briarproject.api.blogs.BlogPostHeader;
import org.briarproject.api.blogs.MessageType;
import org.briarproject.api.clients.Client;
import org.briarproject.api.clients.ClientHelper;
import org.briarproject.api.contact.Contact;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.contact.ContactManager;
import org.briarproject.api.data.BdfDictionary;
import org.briarproject.api.data.BdfEntry;
import org.briarproject.api.data.BdfList;
import org.briarproject.api.data.MetadataParser;
import org.briarproject.api.db.DatabaseComponent;
import org.briarproject.api.db.DbException;
import org.briarproject.api.db.Transaction;
import org.briarproject.api.event.BlogPostAddedEvent;
import org.briarproject.api.identity.Author;
import org.briarproject.api.identity.Author.Status;
import org.briarproject.api.identity.AuthorId;
import org.briarproject.api.identity.IdentityManager;
import org.briarproject.api.identity.IdentityManager.AddIdentityHook;
import org.briarproject.api.identity.IdentityManager.RemoveIdentityHook;
import org.briarproject.api.identity.LocalAuthor;
import org.briarproject.api.sync.ClientId;
import org.briarproject.api.sync.Group;
import org.briarproject.api.sync.GroupId;
import org.briarproject.api.sync.Message;
import org.briarproject.api.sync.MessageId;
import org.briarproject.clients.BdfIncomingMessageHook;
import org.briarproject.util.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.inject.Inject;

import static org.briarproject.api.blogs.BlogConstants.KEY_AUTHOR;
import static org.briarproject.api.blogs.BlogConstants.KEY_AUTHOR_ID;
import static org.briarproject.api.blogs.BlogConstants.KEY_AUTHOR_NAME;
import static org.briarproject.api.blogs.BlogConstants.KEY_COMMENT;
import static org.briarproject.api.blogs.BlogConstants.KEY_DESCRIPTION;
import static org.briarproject.api.blogs.BlogConstants.KEY_ORIGINAL_MSG_ID;
import static org.briarproject.api.blogs.BlogConstants.KEY_ORIGINAL_PARENT_MSG_ID;
import static org.briarproject.api.blogs.BlogConstants.KEY_PARENT_MSG_ID;
import static org.briarproject.api.blogs.BlogConstants.KEY_PUBLIC_KEY;
import static org.briarproject.api.blogs.BlogConstants.KEY_READ;
import static org.briarproject.api.blogs.BlogConstants.KEY_TIMESTAMP;
import static org.briarproject.api.blogs.BlogConstants.KEY_TIME_RECEIVED;
import static org.briarproject.api.blogs.BlogConstants.KEY_TYPE;
import static org.briarproject.api.blogs.MessageType.COMMENT;
import static org.briarproject.api.blogs.MessageType.POST;
import static org.briarproject.api.blogs.MessageType.WRAPPED_COMMENT;
import static org.briarproject.api.blogs.MessageType.WRAPPED_POST;
import static org.briarproject.api.contact.ContactManager.AddContactHook;
import static org.briarproject.api.contact.ContactManager.RemoveContactHook;
import static org.briarproject.blogs.BlogPostValidator.authorToBdfDictionary;

class BlogManagerImpl extends BdfIncomingMessageHook implements BlogManager,
		AddContactHook, RemoveContactHook, Client,
		AddIdentityHook, RemoveIdentityHook {

	private static final Logger LOG =
			Logger.getLogger(BlogManagerImpl.class.getName());

	static final ClientId CLIENT_ID = new ClientId(StringUtils.fromHexString(
			"dafbe56f0c8971365cea4bb5f08ec9a6" +
					"1d686e058b943997b6ff259ba423f613"));

	private final DatabaseComponent db;
	private final IdentityManager identityManager;
	private final ContactManager contactManager;
	private final BlogFactory blogFactory;
	private final BlogPostFactory blogPostFactory;
	private final List<RemoveBlogHook> removeHooks;

	@Inject
	BlogManagerImpl(DatabaseComponent db, IdentityManager identityManager,
			ClientHelper clientHelper, MetadataParser metadataParser,
			ContactManager contactManager, BlogFactory blogFactory,
			BlogPostFactory blogPostFactory) {
		super(clientHelper, metadataParser);

		this.db = db;
		this.identityManager = identityManager;
		this.contactManager = contactManager;
		this.blogFactory = blogFactory;
		this.blogPostFactory = blogPostFactory;
		removeHooks = new CopyOnWriteArrayList<RemoveBlogHook>();
	}

	@Override
	public ClientId getClientId() {
		return CLIENT_ID;
	}

	@Override
	public void createLocalState(Transaction txn) throws DbException {
		// Ensure every identity does have its own personal blog
		// TODO this can probably be removed once #446 is resolved and all users migrated to a new version
		for (LocalAuthor a : db.getLocalAuthors(txn)) {
			Blog b = blogFactory.createPersonalBlog(a);
			Group g = b.getGroup();
			if (!db.containsGroup(txn, g.getId())) {
				db.addGroup(txn, g);
				for (ContactId c : db.getContacts(txn, a.getId())) {
					db.setVisibleToContact(txn, c, g.getId(), true);
				}
			}
		}
		// Ensure that we have the personal blogs of all pre-existing contacts
		for (Contact c : db.getContacts(txn)) addingContact(txn, c);
	}

	@Override
	public void addingContact(Transaction txn, Contact c) throws DbException {
		// get personal blog of the contact
		Blog b = blogFactory.createPersonalBlog(c.getAuthor());
		Group g = b.getGroup();
		if (!db.containsGroup(txn, g.getId())) {
			// add the personal blog of the contact
			db.addGroup(txn, g);
			db.setVisibleToContact(txn, c.getId(), g.getId(), true);

			// share our personal blog with the new contact
			LocalAuthor a = db.getLocalAuthor(txn, c.getLocalAuthorId());
			Blog b2 = blogFactory.createPersonalBlog(a);
			db.setVisibleToContact(txn, c.getId(), b2.getId(), true);
		}
	}

	@Override
	public void removingContact(Transaction txn, Contact c) throws DbException {
		if (c != null) {
			Blog b = blogFactory.createPersonalBlog(c.getAuthor());
			db.removeGroup(txn, b.getGroup());
		}
	}

	@Override
	public void addingIdentity(Transaction txn, LocalAuthor a)
			throws DbException {

		// add a personal blog for the new identity
		LOG.info("New Personal Blog Added.");
		Blog b = blogFactory.createPersonalBlog(a);
		db.addGroup(txn, b.getGroup());
	}

	@Override
	public void removingIdentity(Transaction txn, LocalAuthor a)
			throws DbException {

		// remove the personal blog of that identity
		Blog b = blogFactory.createPersonalBlog(a);
		db.removeGroup(txn, b.getGroup());
	}

	@Override
	protected boolean incomingMessage(Transaction txn, Message m, BdfList list,
			BdfDictionary meta) throws DbException, FormatException {

		GroupId groupId = m.getGroupId();
		MessageType type = getMessageType(meta);

		if (type == POST || type == COMMENT) {
			BlogPostHeader h =
					getPostHeaderFromMetadata(txn, groupId, m.getId(), meta);

			// check that original message IDs match
			if (type == COMMENT) {
				BdfDictionary d = clientHelper
						.getMessageMetadataAsDictionary(txn, h.getParentId());
				byte[] original1 = d.getRaw(KEY_ORIGINAL_MSG_ID);
				byte[] original2 = meta.getRaw(KEY_ORIGINAL_PARENT_MSG_ID);
				if (!Arrays.equals(original1, original2)) {
					throw new FormatException();
				}
			}

			// broadcast event about new post or comment
			BlogPostAddedEvent event =
					new BlogPostAddedEvent(groupId, h, false);
			txn.attach(event);

			// shares message and its dependencies
			return true;
		} else if (type == WRAPPED_COMMENT) {
			// Check that the original message ID in the dependency's metadata
			// matches the original parent ID of the wrapped comment
			MessageId dependencyId =
					new MessageId(meta.getRaw(KEY_PARENT_MSG_ID));
			BdfDictionary d = clientHelper
					.getMessageMetadataAsDictionary(txn, dependencyId);
			byte[] original1 = d.getRaw(KEY_ORIGINAL_MSG_ID);
			byte[] original2 = meta.getRaw(KEY_ORIGINAL_PARENT_MSG_ID);
			if (!Arrays.equals(original1, original2)) {
				throw new FormatException();
			}
		}
		// don't share message until parent arrives
		return false;
	}

	@Override
	public Blog addBlog(LocalAuthor localAuthor, String name,
			String description) throws DbException {

		Blog b = blogFactory
				.createBlog(name, description, localAuthor);
		BdfDictionary metadata = BdfDictionary.of(
				new BdfEntry(KEY_DESCRIPTION, b.getDescription())
		);

		Transaction txn = db.startTransaction(false);
		try {
			db.addGroup(txn, b.getGroup());
			clientHelper.mergeGroupMetadata(txn, b.getId(), metadata);
			txn.setComplete();
		} catch (FormatException e) {
			throw new DbException(e);
		} finally {
			db.endTransaction(txn);
		}
		return b;
	}

	@Override
	public boolean canBeRemoved(GroupId g) throws DbException {
		Transaction txn = db.startTransaction(true);
		try {
			boolean canBeRemoved = canBeRemoved(txn, g);
			txn.setComplete();
			return canBeRemoved;
		} finally {
			db.endTransaction(txn);
		}
	}

	private boolean canBeRemoved(Transaction txn, GroupId g)
			throws DbException {
		boolean canBeRemoved;
		Blog b = getBlog(txn, g);
		AuthorId authorId = b.getAuthor().getId();
		LocalAuthor localAuthor = identityManager.getLocalAuthor(txn);
		canBeRemoved = !contactManager
				.contactExists(txn, authorId, localAuthor.getId());
		return canBeRemoved;
	}

	@Override
	public void removeBlog(Blog b) throws DbException {
		// TODO if this gets used, check for RSS feeds posting into this blog
		Transaction txn = db.startTransaction(false);
		try {
			if (!canBeRemoved(txn, b.getId()))
				throw new DbException();
			for (RemoveBlogHook hook : removeHooks)
				hook.removingBlog(txn, b);
			db.removeGroup(txn, b.getGroup());
			txn.setComplete();
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void addLocalPost(BlogPost p) throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			addLocalPost(txn, p);
			txn.setComplete();
		} finally {
			//noinspection ThrowFromFinallyBlock
			db.endTransaction(txn);
		}
	}

	@Override
	public void addLocalPost(Transaction txn, BlogPost p) throws DbException {
		try {
			BdfDictionary meta = new BdfDictionary();
			meta.put(KEY_TYPE, POST.getInt());
			meta.put(KEY_TIMESTAMP, p.getMessage().getTimestamp());
			meta.put(KEY_AUTHOR, authorToBdfDictionary(p.getAuthor()));
			meta.put(KEY_READ, true);
			clientHelper.addLocalMessage(txn, p.getMessage(), meta, true);

			// broadcast event about new post
			GroupId groupId = p.getMessage().getGroupId();
			MessageId postId = p.getMessage().getId();
			BlogPostHeader h =
					getPostHeaderFromMetadata(txn, groupId, postId, meta);
			BlogPostAddedEvent event =
					new BlogPostAddedEvent(groupId, h, true);
			txn.attach(event);
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void addLocalComment(LocalAuthor author, GroupId groupId,
			@Nullable String comment, BlogPostHeader pOriginalHeader)
			throws DbException {

		MessageType type = pOriginalHeader.getType();
		if (type != POST && type != COMMENT)
			throw new IllegalArgumentException("Comment on unknown type!");

		Transaction txn = db.startTransaction(false);
		try {
			// Wrap post that we are commenting on
			MessageId parentId = wrapMessage(txn, groupId, pOriginalHeader);

			// Get ID of new parent's original message.
			// Assumes that pOriginalHeader is a POST or COMMENT
			MessageId pOriginalId = pOriginalHeader.getId();

			// Create actual comment
			Message message = blogPostFactory
					.createBlogComment(groupId, author, comment, pOriginalId,
							parentId);
			BdfDictionary meta = new BdfDictionary();
			meta.put(KEY_TYPE, COMMENT.getInt());
			if (comment != null) meta.put(KEY_COMMENT, comment);
			meta.put(KEY_TIMESTAMP, message.getTimestamp());
			meta.put(KEY_ORIGINAL_MSG_ID, message.getId());
			meta.put(KEY_ORIGINAL_PARENT_MSG_ID, pOriginalId);
			meta.put(KEY_PARENT_MSG_ID, parentId);
			meta.put(KEY_AUTHOR, authorToBdfDictionary(author));

			// Send comment
			clientHelper.addLocalMessage(txn, message, meta, true);

			// broadcast event
			BlogPostHeader h =
					getPostHeaderFromMetadata(txn, groupId, message.getId(),
							meta);
			BlogPostAddedEvent event = new BlogPostAddedEvent(groupId, h, true);
			txn.attach(event);
			txn.setComplete();
		} catch (FormatException e) {
			throw new DbException(e);
		} catch (GeneralSecurityException e) {
			throw new IllegalArgumentException("Invalid key of author", e);
		} finally {
			//noinspection ThrowFromFinallyBlock
			db.endTransaction(txn);
		}
	}

	private MessageId wrapMessage(Transaction txn, GroupId groupId,
			BlogPostHeader pOriginalHeader)
			throws DbException, FormatException {

		if (groupId.equals(pOriginalHeader.getGroupId())) {
			// We are trying to wrap a post that is already in our group.
			// This is unnecessary, so just return the post's MessageId
			return pOriginalHeader.getId();
		}

		// Get body of message to be wrapped
		BdfList body =
				clientHelper.getMessageAsList(txn, pOriginalHeader.getId());
		long wTimestamp = pOriginalHeader.getTimestamp();
		Message wMessage;

		BdfDictionary meta = new BdfDictionary();
		MessageType type = pOriginalHeader.getType();
		if (type == POST) {
			Group wGroup = db.getGroup(txn, pOriginalHeader.getGroupId());
			byte[] wDescriptor = wGroup.getDescriptor();
			// Wrap post
			wMessage = blogPostFactory
					.wrapPost(groupId, wDescriptor, wTimestamp, body);
			meta.put(KEY_TYPE, WRAPPED_POST.getInt());
		} else if (type == COMMENT) {
			Group wGroup = db.getGroup(txn, pOriginalHeader.getGroupId());
			byte[] wDescriptor = wGroup.getDescriptor();
			BlogCommentHeader wComment = (BlogCommentHeader) pOriginalHeader;
			MessageId wrappedId =
					wrapMessage(txn, groupId, wComment.getParent());
			// Wrap comment
			wMessage = blogPostFactory
					.wrapComment(groupId, wDescriptor, wTimestamp,
							body, wrappedId);
			meta.put(KEY_TYPE, WRAPPED_COMMENT.getInt());
			if(wComment.getComment() != null)
				meta.put(KEY_COMMENT, wComment.getComment());
			meta.put(KEY_PARENT_MSG_ID, wrappedId);
		} else if (type == WRAPPED_POST) {
			// Re-wrap wrapped post without adding another wrapping layer
			wMessage = blogPostFactory.rewrapWrappedPost(groupId, body);
			meta.put(KEY_TYPE, WRAPPED_POST.getInt());
		} else if (type == WRAPPED_COMMENT) {
			BlogCommentHeader wComment = (BlogCommentHeader) pOriginalHeader;
			MessageId wrappedId =
					wrapMessage(txn, groupId, wComment.getParent());
			// Re-wrap wrapped comment
			wMessage = blogPostFactory
					.rewrapWrappedComment(groupId, body, wrappedId);
			meta.put(KEY_TYPE, WRAPPED_COMMENT.getInt());
			if(wComment.getComment() != null)
				meta.put(KEY_COMMENT, wComment.getComment());
			meta.put(KEY_PARENT_MSG_ID, wrappedId);
		} else {
			throw new IllegalArgumentException(
					"Unknown Message Type: " + type);
		}
		meta.put(KEY_ORIGINAL_MSG_ID, pOriginalHeader.getId());
		meta.put(KEY_AUTHOR, authorToBdfDictionary(pOriginalHeader.getAuthor()));
		meta.put(KEY_TIMESTAMP, pOriginalHeader.getTimestamp());
		meta.put(KEY_TIME_RECEIVED, pOriginalHeader.getTimeReceived());

		// Send wrapped message and store metadata
		clientHelper.addLocalMessage(txn, wMessage, meta, true);
		return wMessage.getId();
	}

	@Override
	public Blog getBlog(GroupId g) throws DbException {
		Blog blog;
		Transaction txn = db.startTransaction(true);
		try {
			blog = getBlog(txn, g);
			txn.setComplete();
		} finally {
			db.endTransaction(txn);
		}
		return blog;
	}

	@Override
	public Blog getBlog(Transaction txn, GroupId g) throws DbException {
		try {
			Group group = db.getGroup(txn, g);
			String description = getBlogDescription(txn, g);
			return blogFactory.parseBlog(group, description);
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public Collection<Blog> getBlogs(LocalAuthor localAuthor)
			throws DbException {

		Collection<Blog> allBlogs = getBlogs();
		List<Blog> blogs = new ArrayList<Blog>();
		for (Blog b : allBlogs) {
			if (b.getAuthor().equals(localAuthor)) {
				blogs.add(b);
			}
		}
		return Collections.unmodifiableList(blogs);
	}

	@Override
	public Blog getPersonalBlog(Author author) {
		return blogFactory.createPersonalBlog(author);
	}

	@Override
	public Collection<Blog> getBlogs() throws DbException {
		try {
			List<Blog> blogs = new ArrayList<Blog>();
			Collection<Group> groups;
			Transaction txn = db.startTransaction(true);
			try {
				groups = db.getGroups(txn, CLIENT_ID);
				for (Group g : groups) {
					String description = getBlogDescription(txn, g.getId());
					blogs.add(blogFactory.parseBlog(g, description));
				}
				txn.setComplete();
			} finally {
				db.endTransaction(txn);
			}
			return Collections.unmodifiableList(blogs);
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public BlogPostHeader getPostHeader(GroupId g, MessageId m)
			throws DbException {
		Transaction txn = db.startTransaction(true);
		try {
			BdfDictionary meta =
					clientHelper.getMessageMetadataAsDictionary(txn, m);
			BlogPostHeader h = getPostHeaderFromMetadata(txn, g, m, meta);
			txn.setComplete();
			return h;
		} catch (FormatException e) {
			throw new DbException(e);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public String getPostBody(MessageId m) throws DbException {
		try {
			BdfList message = clientHelper.getMessageAsList(m);
			if (message == null) throw new DbException();
			return getPostBody(message);
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	private String getPostBody(BdfList message) throws FormatException {
		MessageType type = MessageType.valueOf(message.getLong(0).intValue());
		if (type == POST) {
			// type, body, signature
			return message.getString(1);
		} else if (type == WRAPPED_POST) {
			// type, p_group descriptor, p_timestamp, p_content, p_signature
			return message.getString(3);
		} else {
			throw new FormatException();
		}
	}

	@Override
	public Collection<BlogPostHeader> getPostHeaders(GroupId g)
			throws DbException {

		// Query for posts and comments only
		BdfDictionary query1 = BdfDictionary.of(
				new BdfEntry(KEY_TYPE, POST.getInt())
		);
		BdfDictionary query2 = BdfDictionary.of(
				new BdfEntry(KEY_TYPE, COMMENT.getInt())
		);

		Collection<BlogPostHeader> headers = new ArrayList<BlogPostHeader>();
		Transaction txn = db.startTransaction(true);
		try {
			Map<MessageId, BdfDictionary> metadata1 =
					clientHelper.getMessageMetadataAsDictionary(txn, g, query1);
			Map<MessageId, BdfDictionary> metadata2 =
					clientHelper.getMessageMetadataAsDictionary(txn, g, query2);
			Map<MessageId, BdfDictionary> metadata =
					new HashMap<MessageId, BdfDictionary>(
							metadata1.size() + metadata2.size());
			metadata.putAll(metadata1);
			metadata.putAll(metadata2);
			// get all authors we need to get the status for
			Set<AuthorId> authors = new HashSet<AuthorId>();
			for (Entry<MessageId, BdfDictionary> entry : metadata.entrySet()) {
				authors.add(new AuthorId(
						entry.getValue().getDictionary(KEY_AUTHOR)
								.getRaw(KEY_AUTHOR_ID)));
			}
			// get statuses for all authors
			Map<AuthorId, Status> authorStatuses =
					new HashMap<AuthorId, Status>();
			for (AuthorId authorId : authors) {
				authorStatuses.put(authorId,
						identityManager.getAuthorStatus(txn, authorId));
			}
			// get post headers
			for (Entry<MessageId, BdfDictionary> entry : metadata.entrySet()) {
				BdfDictionary meta = entry.getValue();
				BlogPostHeader h =
						getPostHeaderFromMetadata(txn, g, entry.getKey(), meta,
								authorStatuses);
				headers.add(h);
			}
			txn.setComplete();
			return headers;
		} catch (FormatException e) {
			throw new DbException(e);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void setReadFlag(MessageId m, boolean read) throws DbException {
		try {
			BdfDictionary meta = new BdfDictionary();
			meta.put(KEY_READ, read);
			clientHelper.mergeMessageMetadata(m, meta);
		} catch (FormatException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void registerRemoveBlogHook(RemoveBlogHook hook) {
		removeHooks.add(hook);
	}

	private String getBlogDescription(Transaction txn, GroupId g)
			throws DbException, FormatException {
		BdfDictionary d = clientHelper.getGroupMetadataAsDictionary(txn, g);
		return d.getString(KEY_DESCRIPTION, "");
	}

	private BlogPostHeader getPostHeaderFromMetadata(Transaction txn,
			GroupId groupId, MessageId id) throws DbException, FormatException {
		BdfDictionary meta =
				clientHelper.getMessageMetadataAsDictionary(txn, id);
		return getPostHeaderFromMetadata(txn, groupId, id, meta);
	}

	private BlogPostHeader getPostHeaderFromMetadata(Transaction txn,
			GroupId groupId, MessageId id, BdfDictionary meta)
			throws DbException, FormatException {
		return getPostHeaderFromMetadata(txn, groupId, id, meta,
				Collections.<AuthorId, Status>emptyMap());
	}

	private BlogPostHeader getPostHeaderFromMetadata(Transaction txn,
			GroupId groupId, MessageId id, BdfDictionary meta,
			Map<AuthorId, Status> authorStatuses)
			throws DbException, FormatException {

		MessageType type = getMessageType(meta);

		long timestamp = meta.getLong(KEY_TIMESTAMP);
		long timeReceived = meta.getLong(KEY_TIME_RECEIVED, timestamp);

		BdfDictionary d = meta.getDictionary(KEY_AUTHOR);
		AuthorId authorId = new AuthorId(d.getRaw(KEY_AUTHOR_ID));
		String name = d.getString(KEY_AUTHOR_NAME);
		byte[] publicKey = d.getRaw(KEY_PUBLIC_KEY);
		Author author = new Author(authorId, name, publicKey);
		Status authorStatus;
		if (authorStatuses.containsKey(authorId)) {
			authorStatus = authorStatuses.get(authorId);
		} else {
			authorStatus = identityManager.getAuthorStatus(txn, authorId);
		}

		boolean read = meta.getBoolean(KEY_READ, false);

		if (type == COMMENT || type == WRAPPED_COMMENT) {
			String comment = meta.getOptionalString(KEY_COMMENT);
			MessageId parentId = new MessageId(meta.getRaw(KEY_PARENT_MSG_ID));
			BlogPostHeader parent =
					getPostHeaderFromMetadata(txn, groupId, parentId);
			return new BlogCommentHeader(type, groupId, comment, parent, id,
					timestamp, timeReceived, author, authorStatus, read);
		} else {
			return new BlogPostHeader(type, groupId, id, timestamp,
					timeReceived, author, authorStatus, read);
		}
	}

	private MessageType getMessageType(BdfDictionary d) throws FormatException {
		Long longType = d.getLong(KEY_TYPE);
		return MessageType.valueOf(longType.intValue());
	}
}

package org.briarproject.sharing;

import org.briarproject.api.clients.SessionId;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.data.BdfDictionary;
import org.briarproject.api.nullsafety.NotNullByDefault;
import org.briarproject.api.sync.GroupId;
import org.briarproject.api.sync.MessageId;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.NotThreadSafe;

import static org.briarproject.api.blogs.BlogConstants.BLOG_AUTHOR_NAME;
import static org.briarproject.api.blogs.BlogConstants.BLOG_PUBLIC_KEY;

@NotThreadSafe
@NotNullByDefault
public class BlogInviteeSessionState extends InviteeSessionState {

	private final String blogAuthorName;
	private final byte[] blogPublicKey;

	public BlogInviteeSessionState(SessionId sessionId, MessageId storageId,
			GroupId groupId, State state, ContactId contactId, GroupId blogId,
			String blogAuthorName, byte[] blogPublicKey,
			@NotNull MessageId invitationId) {
		super(sessionId, storageId, groupId, state, contactId, blogId,
				invitationId);
		this.blogAuthorName = blogAuthorName;
		this.blogPublicKey = blogPublicKey;
	}

	public BdfDictionary toBdfDictionary() {
		BdfDictionary d = super.toBdfDictionary();
		d.put(BLOG_AUTHOR_NAME, getBlogAuthorName());
		d.put(BLOG_PUBLIC_KEY, getBlogPublicKey());
		return d;
	}

	public String getBlogAuthorName() {
		return blogAuthorName;
	}

	public byte[] getBlogPublicKey() {
		return blogPublicKey;
	}
}

package org.libreproject.libre.api.blog;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.identity.LocalAuthor;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;

import java.security.GeneralSecurityException;

import javax.annotation.Nullable;

import static org.libreproject.libre.api.blog.BlogManager.CLIENT_ID;

@NotNullByDefault
public interface BlogPostFactory {

	String SIGNING_LABEL_POST = CLIENT_ID.getString() + "/POST";
	String SIGNING_LABEL_COMMENT = CLIENT_ID.getString() + "/COMMENT";

	BlogPost createBlogPost(GroupId groupId, long timestamp,
			@Nullable MessageId parent, LocalAuthor author, String text)
			throws FormatException, GeneralSecurityException;

	Message createBlogComment(GroupId groupId, LocalAuthor author,
			@Nullable String comment, MessageId parentOriginalId,
			MessageId parentCurrentId)
			throws FormatException, GeneralSecurityException;

	/**
	 * Wraps a blog post
	 */
	Message wrapPost(GroupId groupId, byte[] descriptor, long timestamp,
			BdfList body) throws FormatException;

	/**
	 * Re-wraps a previously wrapped post
	 */
	Message rewrapWrappedPost(GroupId groupId, BdfList body)
			throws FormatException;

	/**
	 * Wraps a blog comment
	 */
	Message wrapComment(GroupId groupId, byte[] descriptor, long timestamp,
			BdfList body, MessageId parentCurrentId) throws FormatException;

	/**
	 * Re-wraps a previously wrapped comment
	 */
	Message rewrapWrappedComment(GroupId groupId, BdfList body,
			MessageId parentCurrentId) throws FormatException;
}

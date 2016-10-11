package org.briarproject.api.forum;

import org.briarproject.api.FormatException;
import org.briarproject.api.identity.LocalAuthor;
import org.briarproject.api.sync.GroupId;
import org.briarproject.api.sync.MessageId;

import java.security.GeneralSecurityException;

public interface ForumPostFactory {

	ForumPost createAnonymousPost(GroupId groupId, long timestamp,
			MessageId parent, String contentType, byte[] body)
			throws FormatException;

	ForumPost createPseudonymousPost(GroupId groupId, long timestamp,
			MessageId parent, LocalAuthor author, String body)
			throws FormatException, GeneralSecurityException;
}

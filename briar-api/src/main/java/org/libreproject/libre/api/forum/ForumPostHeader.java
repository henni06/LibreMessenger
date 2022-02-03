package org.libreproject.libre.api.forum;

import org.libreproject.bramble.api.identity.Author;
import org.libreproject.libre.api.identity.AuthorInfo;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.client.PostHeader;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class ForumPostHeader extends PostHeader {

	public ForumPostHeader(MessageId id, @Nullable MessageId parentId,
			long timestamp, Author author, AuthorInfo authorInfo,
			boolean read) {
		super(id, parentId, timestamp, author, authorInfo, read);
	}

}

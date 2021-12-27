package org.briarproject.briar.api.privategroup.location;

import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.briar.api.client.PostHeader;
import org.briarproject.briar.api.identity.AuthorInfo;
import org.briarproject.briar.api.privategroup.GroupMessageHeader;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class LocationMessageHeader extends GroupMessageHeader {

	private final GroupId groupId;

	/*public LocationMessageHeader(GroupId groupId, MessageId id,
			@Nullable MessageId parentId, long timestamp,
			Author author, AuthorInfo authorInfo, boolean read) {
		super(id, parentId, timestamp, author, authorInfo, read);
		this.groupId = groupId;
	}*/

	public LocationMessageHeader(GroupMessageHeader h) {
		super(h.getGroupId(), h.getId(), h.getParentId(), h.getTimestamp(),
				h.getAuthor(), h.getAuthorInfo(), h.isRead());
		groupId=h.getGroupId();
	}

	public GroupId getGroupId() {
		return groupId;
	}

}

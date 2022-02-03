package org.libreproject.libre.api.privategroup.location;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.api.privategroup.GroupMessageHeader;

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

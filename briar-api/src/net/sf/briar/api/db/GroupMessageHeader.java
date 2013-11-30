package net.sf.briar.api.db;

import net.sf.briar.api.Author;
import net.sf.briar.api.messaging.GroupId;
import net.sf.briar.api.messaging.MessageId;

public class GroupMessageHeader extends MessageHeader {

	private final GroupId groupId;

	public GroupMessageHeader(MessageId id, MessageId parent, Author author,
			String contentType, long timestamp, boolean read,
			GroupId groupId) {
		super(id, parent, author, contentType, timestamp, read);
		this.groupId = groupId;
	}

	/** Returns the ID of the group to which the message belongs. */
	public GroupId getGroupId() {
		return groupId;
	}
}

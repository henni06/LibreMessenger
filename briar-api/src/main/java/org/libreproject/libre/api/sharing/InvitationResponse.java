package org.libreproject.libre.api.sharing;

import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.client.SessionId;
import org.libreproject.libre.api.conversation.ConversationResponse;

public abstract class InvitationResponse extends ConversationResponse {

	private final GroupId shareableId;

	public InvitationResponse(MessageId id, GroupId groupId, long time,
			boolean local, boolean read, boolean sent, boolean seen,
			SessionId sessionId, boolean accepted, GroupId shareableId,
			long autoDeleteTimer, boolean isAutoDecline) {
		super(id, groupId, time, local, read, sent, seen, sessionId, accepted,
				autoDeleteTimer, isAutoDecline);
		this.shareableId = shareableId;
	}

	public GroupId getShareableId() {
		return shareableId;
	}

}

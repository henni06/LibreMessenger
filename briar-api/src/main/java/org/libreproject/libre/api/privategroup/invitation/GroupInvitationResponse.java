package org.libreproject.libre.api.privategroup.invitation;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.client.SessionId;
import org.libreproject.libre.api.conversation.ConversationMessageVisitor;
import org.libreproject.libre.api.sharing.InvitationResponse;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class GroupInvitationResponse extends InvitationResponse {

	public GroupInvitationResponse(MessageId id, GroupId groupId, long time,
			boolean local, boolean read, boolean sent, boolean seen,
			SessionId sessionId, boolean accept, GroupId shareableId,
			long autoDeleteTimer, boolean isAutoDecline) {
		super(id, groupId, time, local, read, sent, seen, sessionId,
				accept, shareableId, autoDeleteTimer, isAutoDecline);
	}

	@Override
	public <T> T accept(ConversationMessageVisitor<T> v) {
		return v.visitGroupInvitationResponse(this);
	}
}

package org.libreproject.libre.api.privategroup.invitation;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.client.SessionId;
import org.libreproject.libre.api.conversation.ConversationMessageVisitor;
import org.libreproject.libre.api.privategroup.PrivateGroup;
import org.libreproject.libre.api.sharing.InvitationRequest;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class GroupInvitationRequest extends InvitationRequest<PrivateGroup> {

	public GroupInvitationRequest(MessageId id, GroupId groupId, long time,
			boolean local, boolean read, boolean sent, boolean seen,
			SessionId sessionId, PrivateGroup shareable,
			@Nullable String text, boolean available, boolean canBeOpened,
			long autoDeleteTimer) {
		super(id, groupId, time, local, read, sent, seen, sessionId, shareable,
				text, available, canBeOpened, autoDeleteTimer);
	}

	@Override
	public <T> T accept(ConversationMessageVisitor<T> v) {
		return v.visitGroupInvitationRequest(this);
	}
}

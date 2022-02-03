package org.libreproject.libre.api.sharing;

import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.client.SessionId;
import org.libreproject.libre.api.conversation.ConversationRequest;

import javax.annotation.Nullable;

public abstract class InvitationRequest<S extends Shareable> extends
		ConversationRequest<S> {

	private final boolean canBeOpened;

	public InvitationRequest(MessageId messageId, GroupId groupId, long time,
			boolean local, boolean read, boolean sent, boolean seen,
			SessionId sessionId, S object, @Nullable String text,
			boolean available, boolean canBeOpened, long autoDeleteTimer) {
		super(messageId, groupId, time, local, read, sent, seen, sessionId,
				object, text, !available, autoDeleteTimer);
		this.canBeOpened = canBeOpened;
	}

	public boolean canBeOpened() {
		return canBeOpened;
	}
}

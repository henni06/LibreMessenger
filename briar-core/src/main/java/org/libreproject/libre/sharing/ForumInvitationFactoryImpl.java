package org.libreproject.libre.sharing;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.client.SessionId;
import org.libreproject.libre.api.forum.Forum;
import org.libreproject.libre.api.forum.ForumInvitationRequest;
import org.libreproject.libre.api.forum.ForumInvitationResponse;

import javax.inject.Inject;

public class ForumInvitationFactoryImpl
		implements InvitationFactory<Forum, ForumInvitationResponse> {

	@Inject
	ForumInvitationFactoryImpl() {
	}

	@Override
	public ForumInvitationRequest createInvitationRequest(boolean local,
			boolean sent, boolean seen, boolean read, InviteMessage<Forum> m,
			ContactId c, boolean available, boolean canBeOpened,
			long autoDeleteTimer) {
		SessionId sessionId = new SessionId(m.getShareableId().getBytes());
		return new ForumInvitationRequest(m.getId(), m.getContactGroupId(),
				m.getTimestamp(), local, read, sent, seen, sessionId,
				m.getShareable(), m.getText(), available, canBeOpened,
				autoDeleteTimer);
	}

	@Override
	public ForumInvitationResponse createInvitationResponse(MessageId id,
			GroupId contactGroupId, long time, boolean local, boolean sent,
			boolean seen, boolean read, boolean accept, GroupId shareableId,
			long autoDeleteTimer, boolean isAutoDecline) {
		SessionId sessionId = new SessionId(shareableId.getBytes());
		return new ForumInvitationResponse(id, contactGroupId, time, local,
				read, sent, seen, sessionId, accept, shareableId,
				autoDeleteTimer, isAutoDecline);
	}

}

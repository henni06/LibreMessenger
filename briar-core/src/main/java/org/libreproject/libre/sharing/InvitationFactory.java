package org.libreproject.libre.sharing;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.conversation.ConversationRequest;
import org.libreproject.libre.api.sharing.InvitationResponse;
import org.libreproject.libre.api.sharing.Shareable;

public interface InvitationFactory<S extends Shareable, R extends InvitationResponse> {

	ConversationRequest<S> createInvitationRequest(boolean local, boolean sent,
			boolean seen, boolean read, InviteMessage<S> m, ContactId c,
			boolean available, boolean canBeOpened, long autoDeleteTimer);

	R createInvitationResponse(MessageId id, GroupId contactGroupId, long time,
			boolean local, boolean sent, boolean seen, boolean read,
			boolean accept, GroupId shareableId, long autoDeleteTimer,
			boolean isAutoDecline);

}

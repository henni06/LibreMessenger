package org.libreproject.libre.api.privategroup.event;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.conversation.event.ConversationMessageReceivedEvent;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationRequest;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class GroupInvitationRequestReceivedEvent extends
		ConversationMessageReceivedEvent<GroupInvitationRequest> {

	public GroupInvitationRequestReceivedEvent(GroupInvitationRequest request,
			ContactId contactId) {
		super(request, contactId);
	}

}

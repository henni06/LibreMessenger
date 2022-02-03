package org.libreproject.libre.api.privategroup.event;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.conversation.event.ConversationMessageReceivedEvent;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationResponse;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class GroupInvitationResponseReceivedEvent
		extends ConversationMessageReceivedEvent<GroupInvitationResponse> {

	public GroupInvitationResponseReceivedEvent(
			GroupInvitationResponse response, ContactId contactId) {
		super(response, contactId);
	}
}

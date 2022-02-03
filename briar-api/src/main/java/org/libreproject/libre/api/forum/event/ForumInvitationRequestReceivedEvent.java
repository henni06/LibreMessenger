package org.libreproject.libre.api.forum.event;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.conversation.ConversationRequest;
import org.libreproject.libre.api.conversation.event.ConversationMessageReceivedEvent;
import org.libreproject.libre.api.forum.Forum;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class ForumInvitationRequestReceivedEvent extends
		ConversationMessageReceivedEvent<ConversationRequest<Forum>> {

	public ForumInvitationRequestReceivedEvent(ConversationRequest<Forum> request,
			ContactId contactId) {
		super(request, contactId);
	}

}

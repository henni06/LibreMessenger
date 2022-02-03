package org.libreproject.libre.api.blog.event;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.blog.BlogInvitationResponse;
import org.libreproject.libre.api.conversation.event.ConversationMessageReceivedEvent;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class BlogInvitationResponseReceivedEvent
		extends ConversationMessageReceivedEvent<BlogInvitationResponse> {

	public BlogInvitationResponseReceivedEvent(BlogInvitationResponse response,
			ContactId contactId) {
		super(response, contactId);
	}

}

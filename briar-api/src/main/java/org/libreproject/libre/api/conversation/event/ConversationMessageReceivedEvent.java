package org.libreproject.libre.api.conversation.event;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.conversation.ConversationMessageHeader;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a new conversation message is received.
 */
@Immutable
@NotNullByDefault
public abstract class ConversationMessageReceivedEvent<H extends ConversationMessageHeader>
		extends Event {

	private final H messageHeader;
	private final ContactId contactId;

	public ConversationMessageReceivedEvent(H messageHeader,
			ContactId contactId) {
		this.messageHeader = messageHeader;
		this.contactId = contactId;
	}

	public H getMessageHeader() {
		return messageHeader;
	}

	public ContactId getContactId() {
		return contactId;
	}
}

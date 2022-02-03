package org.libreproject.libre.api.messaging.event;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.MessageId;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a new attachment is received.
 */
@Immutable
@NotNullByDefault
public class AttachmentReceivedEvent extends Event {

	private final MessageId messageId;
	private final ContactId contactId;

	public AttachmentReceivedEvent(MessageId messageId, ContactId contactId) {
		this.messageId = messageId;
		this.contactId = contactId;
	}

	public MessageId getMessageId() {
		return messageId;
	}

	public ContactId getContactId() {
		return contactId;
	}
}

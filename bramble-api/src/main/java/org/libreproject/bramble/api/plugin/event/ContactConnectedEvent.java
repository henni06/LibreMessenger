package org.libreproject.bramble.api.plugin.event;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a contact connects that was not previously
 * connected via any transport.
 */
@Immutable
@NotNullByDefault
public class ContactConnectedEvent extends Event {

	private final ContactId contactId;

	public ContactConnectedEvent(ContactId contactId) {
		this.contactId = contactId;
	}

	public ContactId getContactId() {
		return contactId;
	}
}

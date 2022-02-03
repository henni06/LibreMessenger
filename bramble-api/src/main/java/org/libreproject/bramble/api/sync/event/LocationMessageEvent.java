package org.libreproject.bramble.api.sync.event;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.sync.Message;

import javax.annotation.Nullable;

public class LocationMessageEvent extends Event {
	public Message getMessage() {
		return message;
	}

	@Nullable
	public ContactId getContactId() {
		return contactId;
	}

	private final Message message;
	@Nullable
	private final ContactId contactId;

	public LocationMessageEvent(Message message,ContactId contactId){
		this.message=message;
		this.contactId=contactId;
	}




}

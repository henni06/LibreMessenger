package org.briarproject.bramble.api.sync.event;

import org.briarproject.bramble.api.FormatException;
import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.data.BdfList;
import org.briarproject.bramble.api.data.BdfReader;
import org.briarproject.bramble.api.event.Event;
import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.sync.Message;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.annotation.Nullable;

import static org.briarproject.bramble.api.data.BdfReader.DEFAULT_MAX_BUFFER_SIZE;
import static org.briarproject.bramble.api.data.BdfReader.DEFAULT_NESTED_LIMIT;

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

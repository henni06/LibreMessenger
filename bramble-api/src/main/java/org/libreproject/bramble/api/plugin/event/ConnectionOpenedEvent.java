package org.libreproject.bramble.api.plugin.event;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportId;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class ConnectionOpenedEvent extends Event {

	private final ContactId contactId;
	private final TransportId transportId;
	private final boolean incoming;

	public ConnectionOpenedEvent(ContactId contactId, TransportId transportId,
			boolean incoming) {
		this.contactId = contactId;
		this.transportId = transportId;
		this.incoming = incoming;
	}

	public ContactId getContactId() {
		return contactId;
	}

	public TransportId getTransportId() {
		return transportId;
	}

	public boolean isIncoming() {
		return incoming;
	}
}

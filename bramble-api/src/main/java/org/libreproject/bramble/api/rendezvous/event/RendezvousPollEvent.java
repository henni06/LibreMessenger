package org.libreproject.bramble.api.rendezvous.event;

import org.libreproject.bramble.api.contact.PendingContactId;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportId;

import java.util.Collection;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a transport plugin is polled for connections
 * to one or more pending contacts.
 */
@Immutable
@NotNullByDefault
public class RendezvousPollEvent extends Event {

	private final TransportId transportId;
	private final Collection<PendingContactId> pendingContacts;

	public RendezvousPollEvent(TransportId transportId,
			Collection<PendingContactId> pendingContacts) {
		this.transportId = transportId;
		this.pendingContacts = pendingContacts;
	}

	public TransportId getTransportId() {
		return transportId;
	}

	public Collection<PendingContactId> getPendingContacts() {
		return pendingContacts;
	}
}

package org.libreproject.bramble.api.rendezvous.event;

import org.libreproject.bramble.api.contact.PendingContactId;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a rendezvous connection is opened.
 */
@Immutable
@NotNullByDefault
public class RendezvousConnectionOpenedEvent extends Event {

	private final PendingContactId pendingContactId;

	public RendezvousConnectionOpenedEvent(PendingContactId pendingContactId) {
		this.pendingContactId = pendingContactId;
	}

	public PendingContactId getPendingContactId() {
		return pendingContactId;
	}
}

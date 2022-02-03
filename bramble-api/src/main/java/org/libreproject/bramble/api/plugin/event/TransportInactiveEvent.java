package org.libreproject.bramble.api.plugin.event;

import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.Plugin.State;
import org.libreproject.bramble.api.plugin.TransportId;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a plugin leaves the {@link State#ACTIVE}
 * state.
 */
@Immutable
@NotNullByDefault
public class TransportInactiveEvent extends Event {

	private final TransportId transportId;

	public TransportInactiveEvent(TransportId transportId) {
		this.transportId = transportId;
	}

	public TransportId getTransportId() {
		return transportId;
	}
}

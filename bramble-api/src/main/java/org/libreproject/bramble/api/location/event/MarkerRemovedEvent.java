package org.libreproject.bramble.api.location.event;

import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a marker is removed.
 */
@Immutable
@NotNullByDefault
public class MarkerRemovedEvent extends Event {

	private final String markerID;

	public MarkerRemovedEvent(String markerID) {
		this.markerID=markerID;
	}

	public String getMarkerID() {
		return markerID;
	}

}

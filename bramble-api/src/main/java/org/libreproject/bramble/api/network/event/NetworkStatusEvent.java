package org.libreproject.bramble.api.network.event;

import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.network.NetworkStatus;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class NetworkStatusEvent extends Event {

	private final NetworkStatus status;

	public NetworkStatusEvent(NetworkStatus status) {
		this.status = status;
	}

	public NetworkStatus getStatus() {
		return status;
	}
}
package org.libreproject.bramble.api.keyagreement.event;

import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a BQP protocol aborts.
 */
@Immutable
@NotNullByDefault
public class KeyAgreementAbortedEvent extends Event {

	private final boolean remoteAborted;

	public KeyAgreementAbortedEvent(boolean remoteAborted) {
		this.remoteAborted = remoteAborted;
	}

	public boolean didRemoteAbort() {
		return remoteAborted;
	}
}

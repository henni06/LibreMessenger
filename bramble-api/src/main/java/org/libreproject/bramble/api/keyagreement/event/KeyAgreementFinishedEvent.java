package org.libreproject.bramble.api.keyagreement.event;

import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.keyagreement.KeyAgreementResult;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a BQP protocol completes.
 */
@Immutable
@NotNullByDefault
public class KeyAgreementFinishedEvent extends Event {

	private final KeyAgreementResult result;

	public KeyAgreementFinishedEvent(KeyAgreementResult result) {
		this.result = result;
	}

	public KeyAgreementResult getResult() {
		return result;
	}
}

package org.libreproject.bramble.transport.agreement;

import org.libreproject.bramble.api.crypto.KeyPair;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.api.transport.KeySetId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class Session {

	private final State state;
	@Nullable
	private final MessageId lastLocalMessageId;
	@Nullable
	private final KeyPair localKeyPair;
	@Nullable
	private final Long localTimestamp;
	@Nullable
	private final KeySetId keySetId;

	Session(State state, @Nullable MessageId lastLocalMessageId,
			@Nullable KeyPair localKeyPair, @Nullable Long localTimestamp,
			@Nullable KeySetId keySetId) {
		this.state = state;
		this.lastLocalMessageId = lastLocalMessageId;
		this.localKeyPair = localKeyPair;
		this.localTimestamp = localTimestamp;
		this.keySetId = keySetId;
	}

	State getState() {
		return state;
	}

	@Nullable
	MessageId getLastLocalMessageId() {
		return lastLocalMessageId;
	}

	@Nullable
	KeyPair getLocalKeyPair() {
		return localKeyPair;
	}

	@Nullable
	Long getLocalTimestamp() {
		return localTimestamp;
	}

	@Nullable
	KeySetId getKeySetId() {
		return keySetId;
	}
}

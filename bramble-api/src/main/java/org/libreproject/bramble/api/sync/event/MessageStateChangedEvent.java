package org.libreproject.bramble.api.sync.event;

import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.api.sync.validation.MessageState;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a message state changed.
 */
@Immutable
@NotNullByDefault
public class MessageStateChangedEvent extends Event {

	private final MessageId messageId;
	private final boolean local;
	private final MessageState state;

	public MessageStateChangedEvent(MessageId messageId, boolean local,
			MessageState state) {
		this.messageId = messageId;
		this.local = local;
		this.state = state;
	}

	public MessageId getMessageId() {
		return messageId;
	}

	public boolean isLocal() {
		return local;
	}

	public MessageState getState() {
		return state;
	}

}

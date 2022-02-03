package org.libreproject.bramble.api.sync.event;

import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.MessageId;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a message is shared.
 */
@Immutable
@NotNullByDefault
public class MessageSharedEvent extends Event {

	private final MessageId messageId;

	public MessageSharedEvent(MessageId message) {
		this.messageId = message;
	}

	public MessageId getMessageId() {
		return messageId;
	}
}

package org.libreproject.bramble.api.sync;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface MessageFactory {

	Message createMessage(GroupId g, long timestamp, byte[] body,
			Message.MessageType messageType);

	Message createMessage(byte[] raw, Message.MessageType messageType);

	byte[] getRawMessage(Message m);
}

package org.libreproject.bramble.test;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageFactory;

import static org.libreproject.bramble.api.sync.SyncConstants.MESSAGE_HEADER_LENGTH;

@NotNullByDefault
public class TestMessageFactory implements MessageFactory {

	@Override
	public Message createMessage(GroupId g, long timestamp, byte[] body,Message.MessageType messageType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Message createMessage(byte[] raw, Message.MessageType messageType) {
		throw new UnsupportedOperationException();
	}




	@Override
	public byte[] getRawMessage(Message m) {
		byte[] body = m.getBody();
		byte[] raw = new byte[MESSAGE_HEADER_LENGTH + body.length];
		System.arraycopy(body, 0, raw, MESSAGE_HEADER_LENGTH, body.length);
		return raw;
	}
}

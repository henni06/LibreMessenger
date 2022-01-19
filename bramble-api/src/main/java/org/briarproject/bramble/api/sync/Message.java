package org.briarproject.bramble.api.sync;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

import static org.briarproject.bramble.api.sync.SyncConstants.MAX_MESSAGE_BODY_LENGTH;
import static org.briarproject.bramble.api.sync.SyncConstants.MESSAGE_HEADER_LENGTH;

@Immutable
@NotNullByDefault
public class Message {

	public static final String LOCATION_IDENTIFIER = "{\"type\":\"location\"";
	/**
	 * The current version of the message format.
	 */
	public static final int FORMAT_VERSION = 1;
	public enum  MessageType{DEFAULT,LOCATION,MARKER};

	private final MessageId id;
	private final GroupId groupId;
	private final long timestamp;
	private final byte[] body;
	private final MessageType messageType;

	public Message(MessageId id, GroupId groupId, long timestamp, byte[] body,MessageType messageType) {
		if (body.length == 0) throw new IllegalArgumentException();
		if (body.length > MAX_MESSAGE_BODY_LENGTH)
			throw new IllegalArgumentException();
		this.id = id;
		this.groupId = groupId;
		this.timestamp = timestamp;
		this.body = body;
		this.messageType=messageType;
	}

	/**
	 * Returns the message's unique identifier.
	 */
	public MessageId getId() {
		return id;
	}

	public MessageType getMessageType(){ return messageType;}

	/**
	 * Returns the ID of the {@link Group} to which the message belongs.
	 */
	public GroupId getGroupId() {
		return groupId;
	}

	/**
	 * Returns the message's timestamp in milliseconds since the Unix epoch.
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Returns the length of the raw message in bytes.
	 */
	public int getRawLength() {
		return MESSAGE_HEADER_LENGTH + body.length;
	}

	/**
	 * Returns the message body.
	 */
	public byte[] getBody() {
		return body;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Message && id.equals(((Message) o).getId());
	}
}
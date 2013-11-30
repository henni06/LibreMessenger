package net.sf.briar.api.db;

import net.sf.briar.api.Author;
import net.sf.briar.api.messaging.MessageId;

public abstract class MessageHeader {

	private final MessageId id, parent;
	private final Author author;
	private final String contentType;
	private final long timestamp;
	private final boolean read;

	protected MessageHeader(MessageId id, MessageId parent, Author author,
			String contentType, long timestamp, boolean read) {
		this.id = id;
		this.parent = parent;
		this.author = author;
		this.contentType = contentType;
		this.timestamp = timestamp;
		this.read = read;
	}

	/** Returns the message's unique identifier. */
	public MessageId getId() {
		return id;
	}

	/**
	 * Returns the message's parent, or null if this is the first message in a
	 * thread.
	 */
	public MessageId getParent() {
		return parent;
	}

	/**
	 * Returns the message's author, or null if this is an  anonymous message.
	 */
	public Author getAuthor() {
		return author;
	}

	/** Returns the message's content type. */
	public String getContentType() {
		return contentType;
	}

	/** Returns the timestamp created by the message's author. */
	public long getTimestamp() {
		return timestamp;
	}

	/** Returns true if the message has been read. */
	public boolean isRead() {
		return read;
	}
}

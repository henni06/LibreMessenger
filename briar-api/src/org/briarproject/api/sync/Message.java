package org.briarproject.api.sync;

import org.briarproject.api.identity.Author;

public interface Message {

	/** Returns the message's unique identifier. */
	MessageId getId();

	/**
	 * Returns the identifier of the message's parent, or null if this is the
	 * first message in a thread.
	 */
	MessageId getParent();

	/**
	 * Returns the {@link Group} to which the message belongs, or null if this
	 * is a private message.
	 */
	Group getGroup();

	/**
	 * Returns the message's {@link Author Author}, or null
	 * if this is an anonymous message.
	 */
	Author getAuthor();

	/** Returns the message's content type. */
	String getContentType();

	/** Returns the message's timestamp in milliseconds since the Unix epoch. */
	long getTimestamp();

	/** Returns the serialised message. */
	byte[] getSerialised();

	/** Returns the offset of the message body within the serialised message. */
	int getBodyStart();

	/** Returns the length of the message body in bytes. */
	int getBodyLength();
}
package org.libreproject.libre.api.client;

import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public abstract class ThreadedMessage {

	private final Message message;
	@Nullable
	private final MessageId parent;
	private final Author author;

	public ThreadedMessage(Message message, @Nullable MessageId parent,
			Author author) {
		this.message = message;
		this.parent = parent;
		this.author = author;
	}

	public Message getMessage() {
		return message;
	}

	@Nullable
	public MessageId getParent() {
		return parent;
	}

	public Author getAuthor() {
		return author;
	}

}

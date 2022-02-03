package org.libreproject.libre.api.forum;

import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.client.ThreadedMessage;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class ForumPost extends ThreadedMessage {

	public ForumPost(Message message, @Nullable MessageId parent,
			Author author) {
		super(message, parent, author);
	}

}

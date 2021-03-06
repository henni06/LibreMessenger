package org.libreproject.libre.introduction;

import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class RequestMessage extends AbstractIntroductionMessage {

	private final Author author;
	@Nullable
	private final String text;

	RequestMessage(MessageId messageId, GroupId groupId, long timestamp,
			@Nullable MessageId previousMessageId, Author author,
			@Nullable String text, long autoDeleteTimer) {
		super(messageId, groupId, timestamp, previousMessageId,
				autoDeleteTimer);
		this.author = author;
		this.text = text;
	}

	public Author getAuthor() {
		return author;
	}

	@Nullable
	public String getText() {
		return text;
	}

}

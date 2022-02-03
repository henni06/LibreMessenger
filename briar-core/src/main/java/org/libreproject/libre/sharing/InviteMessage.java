package org.libreproject.libre.sharing;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.sharing.Shareable;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class InviteMessage<S extends Shareable> extends DeletableSharingMessage {

	private final S shareable;
	@Nullable
	private final String text;

	InviteMessage(MessageId id, @Nullable MessageId previousMessageId,
			GroupId contactGroupId, S shareable, @Nullable String text,
			long timestamp, long autoDeleteTimer) {
		super(id, contactGroupId, shareable.getId(), timestamp,
				previousMessageId, autoDeleteTimer);
		if (text != null && text.isEmpty())
			throw new IllegalArgumentException();
		this.shareable = shareable;
		this.text = text;
	}

	public S getShareable() {
		return shareable;
	}

	@Nullable
	public String getText() {
		return text;
	}

}

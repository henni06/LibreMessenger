package org.libreproject.libre.introduction;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.client.SessionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.libreproject.libre.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;

@Immutable
@NotNullByDefault
class AbortMessage extends AbstractIntroductionMessage {

	private final SessionId sessionId;

	protected AbortMessage(MessageId messageId, GroupId groupId, long timestamp,
			@Nullable MessageId previousMessageId, SessionId sessionId) {
		super(messageId, groupId, timestamp, previousMessageId,
				NO_AUTO_DELETE_TIMER);
		this.sessionId = sessionId;
	}

	public SessionId getSessionId() {
		return sessionId;
	}

}

package org.libreproject.libre.introduction;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.client.SessionId;

import javax.annotation.Nullable;

@NotNullByDefault
interface PeerSession {

	SessionId getSessionId();

	GroupId getContactGroupId();

	long getLocalTimestamp();

	@Nullable
	MessageId getLastLocalMessageId();

	@Nullable
	MessageId getLastRemoteMessageId();

}

package org.libreproject.libre.introduction;

import org.libreproject.bramble.api.crypto.PublicKey;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.properties.TransportProperties;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.client.SessionId;

import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class AcceptMessage extends AbstractIntroductionMessage {

	private final SessionId sessionId;
	private final PublicKey ephemeralPublicKey;
	private final long acceptTimestamp;
	private final Map<TransportId, TransportProperties> transportProperties;

	protected AcceptMessage(MessageId messageId, GroupId groupId,
			long timestamp, @Nullable MessageId previousMessageId,
			SessionId sessionId, PublicKey ephemeralPublicKey,
			long acceptTimestamp,
			Map<TransportId, TransportProperties> transportProperties,
			long autoDeleteTimer) {
		super(messageId, groupId, timestamp, previousMessageId,
				autoDeleteTimer);
		this.sessionId = sessionId;
		this.ephemeralPublicKey = ephemeralPublicKey;
		this.acceptTimestamp = acceptTimestamp;
		this.transportProperties = transportProperties;
	}

	public SessionId getSessionId() {
		return sessionId;
	}

	public PublicKey getEphemeralPublicKey() {
		return ephemeralPublicKey;
	}

	public long getAcceptTimestamp() {
		return acceptTimestamp;
	}

	public Map<TransportId, TransportProperties> getTransportProperties() {
		return transportProperties;
	}

}

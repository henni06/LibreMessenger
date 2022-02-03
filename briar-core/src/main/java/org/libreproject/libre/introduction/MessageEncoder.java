package org.libreproject.libre.introduction;

import org.libreproject.bramble.api.crypto.PublicKey;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.properties.TransportProperties;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.client.SessionId;

import java.util.Map;

import javax.annotation.Nullable;

@NotNullByDefault
interface MessageEncoder {

	BdfDictionary encodeRequestMetadata(long timestamp,
			long autoDeleteTimer);

	BdfDictionary encodeMetadata(MessageType type,
			@Nullable SessionId sessionId, long timestamp,
			long autoDeleteTimer);

	BdfDictionary encodeMetadata(MessageType type,
			@Nullable SessionId sessionId, long timestamp, boolean local,
			boolean read, boolean visible, long autoDeleteTimer,
			boolean isAutoDecline);

	void addSessionId(BdfDictionary meta, SessionId sessionId);

	void setVisibleInUi(BdfDictionary meta, boolean visible);

	void setAvailableToAnswer(BdfDictionary meta, boolean available);

	/**
	 * Encodes a request message without an auto-delete timer.
	 */
	Message encodeRequestMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, Author author,
			@Nullable String text);

	/**
	 * Encodes a request message with an optional auto-delete timer. This
	 * requires the contact to support client version 0.1 or higher.
	 */
	Message encodeRequestMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, Author author,
			@Nullable String text, long autoDeleteTimer);

	/**
	 * Encodes an accept message without an auto-delete timer.
	 */
	Message encodeAcceptMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, SessionId sessionId,
			PublicKey ephemeralPublicKey, long acceptTimestamp,
			Map<TransportId, TransportProperties> transportProperties);

	/**
	 * Encodes an accept message with an optional auto-delete timer. This
	 * requires the contact to support client version 0.1 or higher.
	 */
	Message encodeAcceptMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, SessionId sessionId,
			PublicKey ephemeralPublicKey, long acceptTimestamp,
			Map<TransportId, TransportProperties> transportProperties,
			long autoDeleteTimer);

	/**
	 * Encodes a decline message without an auto-delete timer.
	 */
	Message encodeDeclineMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, SessionId sessionId);

	/**
	 * Encodes a decline message with an optional auto-delete timer. This
	 * requires the contact to support client version 0.1 or higher.
	 */
	Message encodeDeclineMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, SessionId sessionId,
			long autoDeleteTimer);

	Message encodeAuthMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, SessionId sessionId,
			byte[] mac, byte[] signature);

	Message encodeActivateMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, SessionId sessionId,
			byte[] mac);

	Message encodeAbortMessage(GroupId contactGroupId, long timestamp,
			@Nullable MessageId previousMessageId, SessionId sessionId);

}

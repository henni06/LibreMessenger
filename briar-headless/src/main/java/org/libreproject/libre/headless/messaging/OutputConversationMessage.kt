package org.libreproject.libre.headless.messaging

import org.libreproject.bramble.api.contact.ContactId
import org.libreproject.bramble.api.sync.MessageId
import org.libreproject.bramble.api.sync.event.MessagesAckedEvent
import org.libreproject.bramble.api.sync.event.MessagesSentEvent
import org.libreproject.libre.api.conversation.ConversationMessageHeader
import org.libreproject.libre.api.conversation.DeletionResult
import org.libreproject.libre.api.messaging.PrivateMessage
import org.libreproject.libre.api.messaging.PrivateMessageHeader
import org.libreproject.libre.headless.json.JsonDict

internal fun ConversationMessageHeader.output(contactId: ContactId) = JsonDict(
    "contactId" to contactId.int,
    "timestamp" to timestamp,
    "read" to isRead,
    "seen" to isSeen,
    "sent" to isSent,
    "local" to isLocal,
    "id" to id.bytes,
    "groupId" to groupId.bytes
)

internal fun ConversationMessageHeader.output(contactId: ContactId, text: String?): JsonDict {
    val dict = output(contactId)
    dict["text"] = text
    return dict
}

internal fun PrivateMessageHeader.output(contactId: ContactId, text: String?) =
    (this as ConversationMessageHeader).output(contactId, text).apply {
        put("type", "PrivateMessage")
    }

/**
 * Use only for outgoing messages that were just sent
 */
internal fun PrivateMessage.output(contactId: ContactId, text: String) = JsonDict(
    "type" to "PrivateMessage",
    "contactId" to contactId.int,
    "timestamp" to message.timestamp,
    "read" to true,
    "seen" to false,
    "sent" to false,
    "local" to true,
    "id" to message.id.bytes,
    "groupId" to message.groupId.bytes,
    "text" to text
)

internal fun DeletionResult.output() = JsonDict(
    "allDeleted" to allDeleted(),
    "hasIntroductionSessionInProgress" to hasIntroductionSessionInProgress(),
    "hasInvitationSessionInProgress" to hasInvitationSessionInProgress(),
    "hasNotAllIntroductionSelected" to hasNotAllIntroductionSelected(),
    "hasNotAllInvitationSelected" to hasNotAllInvitationSelected()
)

internal fun MessagesAckedEvent.output() = JsonDict(
    "contactId" to contactId.int,
    "messageIds" to messageIds.toJson()
)

internal fun MessagesSentEvent.output() = JsonDict(
    "contactId" to contactId.int,
    "messageIds" to messageIds.toJson()
)

internal fun Collection<MessageId>.toJson() = map { it.bytes }

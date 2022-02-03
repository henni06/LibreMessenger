package org.libreproject.libre.headless.contact

import org.libreproject.bramble.api.contact.Contact
import org.libreproject.bramble.api.contact.event.ContactAddedEvent
import org.libreproject.bramble.api.plugin.event.ContactConnectedEvent
import org.libreproject.bramble.api.plugin.event.ContactDisconnectedEvent
import org.libreproject.bramble.identity.output
import org.libreproject.libre.headless.json.JsonDict

internal fun Contact.output(latestMsgTime: Long, connected: Boolean, unreadCount: Int) = JsonDict(
    "contactId" to id.int,
    "author" to author.output(),
    "verified" to isVerified,
    "lastChatActivity" to latestMsgTime,
    "connected" to connected,
    "unreadCount" to unreadCount
).apply {
    alias?.let { put("alias", it) }
    handshakePublicKey?.let { put("handshakePublicKey", it.encoded) }
}

internal fun ContactAddedEvent.output() = JsonDict(
    "contactId" to contactId.int,
    "verified" to isVerified
)

internal fun ContactConnectedEvent.output() = JsonDict(
    "contactId" to contactId.int
)

internal fun ContactDisconnectedEvent.output() = JsonDict(
    "contactId" to contactId.int
)

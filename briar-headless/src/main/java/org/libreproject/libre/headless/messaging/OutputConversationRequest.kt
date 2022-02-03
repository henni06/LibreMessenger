package org.libreproject.libre.headless.messaging

import org.libreproject.bramble.api.contact.ContactId
import org.libreproject.libre.api.blog.BlogInvitationRequest
import org.libreproject.libre.api.conversation.ConversationMessageHeader
import org.libreproject.libre.api.conversation.ConversationRequest
import org.libreproject.libre.api.forum.ForumInvitationRequest
import org.libreproject.libre.api.introduction.IntroductionRequest
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationRequest
import org.libreproject.libre.api.sharing.InvitationRequest
import org.libreproject.libre.headless.json.JsonDict

internal fun ConversationRequest<*>.output(contactId: ContactId): JsonDict {
    val dict = (this as ConversationMessageHeader).output(contactId, text)
    dict.putAll(
        "sessionId" to sessionId.bytes,
        "name" to name,
        "answered" to wasAnswered()
    )
    return dict
}

internal fun IntroductionRequest.output(contactId: ContactId): JsonDict {
    val dict = (this as ConversationRequest<*>).output(contactId)
    dict.putAll(
        "type" to "IntroductionRequest",
        "alreadyContact" to isContact
    )
    return dict
}

internal fun InvitationRequest<*>.output(contactId: ContactId): JsonDict {
    val dict = (this as ConversationRequest<*>).output(contactId)
    dict["canBeOpened"] = canBeOpened()
    return dict
}

internal fun BlogInvitationRequest.output(contactId: ContactId): JsonDict {
    val dict = (this as InvitationRequest<*>).output(contactId)
    dict["type"] = "BlogInvitationRequest"
    return dict
}

internal fun ForumInvitationRequest.output(contactId: ContactId): JsonDict {
    val dict = (this as InvitationRequest<*>).output(contactId)
    dict["type"] = "ForumInvitationRequest"
    return dict
}

internal fun GroupInvitationRequest.output(contactId: ContactId): JsonDict {
    val dict = (this as InvitationRequest<*>).output(contactId)
    dict["type"] = "GroupInvitationRequest"
    return dict
}

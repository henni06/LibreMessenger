package org.libreproject.libre.headless.messaging

import org.libreproject.bramble.api.contact.ContactId
import org.libreproject.bramble.identity.output
import org.libreproject.libre.api.blog.BlogInvitationResponse
import org.libreproject.libre.api.conversation.ConversationMessageHeader
import org.libreproject.libre.api.conversation.ConversationResponse
import org.libreproject.libre.api.forum.ForumInvitationResponse
import org.libreproject.libre.api.introduction.IntroductionResponse
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationResponse
import org.libreproject.libre.api.sharing.InvitationResponse
import org.libreproject.libre.headless.json.JsonDict

internal fun ConversationResponse.output(contactId: ContactId): JsonDict {
    val dict = (this as ConversationMessageHeader).output(contactId)
    dict.putAll(
        "sessionId" to sessionId.bytes,
        "accepted" to wasAccepted()
    )
    return dict
}

internal fun IntroductionResponse.output(contactId: ContactId): JsonDict {
    val dict = (this as ConversationResponse).output(contactId)
    dict.putAll(
        "type" to "IntroductionResponse",
        "introducedAuthor" to introducedAuthor.output(),
        "introducer" to isIntroducer
    )
    return dict
}

internal fun InvitationResponse.output(contactId: ContactId): JsonDict {
    val dict = (this as ConversationResponse).output(contactId)
    dict["shareableId"] = shareableId.bytes
    return dict
}

internal fun BlogInvitationResponse.output(contactId: ContactId): JsonDict {
    val dict = (this as InvitationResponse).output(contactId)
    dict["type"] = "BlogInvitationResponse"
    return dict
}

internal fun ForumInvitationResponse.output(contactId: ContactId): JsonDict {
    val dict = (this as InvitationResponse).output(contactId)
    dict["type"] = "ForumInvitationResponse"
    return dict
}

internal fun GroupInvitationResponse.output(contactId: ContactId): JsonDict {
    val dict = (this as InvitationResponse).output(contactId)
    dict["type"] = "GroupInvitationResponse"
    return dict
}

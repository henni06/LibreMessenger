package org.libreproject.libre.headless.event

import org.libreproject.libre.api.blog.BlogInvitationRequest
import org.libreproject.libre.api.blog.BlogInvitationResponse
import org.libreproject.libre.api.conversation.event.ConversationMessageReceivedEvent
import org.libreproject.libre.api.forum.ForumInvitationRequest
import org.libreproject.libre.api.forum.ForumInvitationResponse
import org.libreproject.libre.api.introduction.IntroductionRequest
import org.libreproject.libre.api.introduction.IntroductionResponse
import org.libreproject.libre.api.messaging.PrivateMessageHeader
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationRequest
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationResponse
import org.libreproject.libre.headless.json.JsonDict
import org.libreproject.libre.headless.messaging.output
import javax.annotation.concurrent.Immutable

@Immutable
@Suppress("unused")
internal class OutputEvent(val name: String, val data: JsonDict) {
    val type = "event"
}

internal fun ConversationMessageReceivedEvent<*>.output(text: String?): JsonDict {
    check(messageHeader is PrivateMessageHeader)
    return (messageHeader as PrivateMessageHeader).output(contactId, text)
}

internal fun ConversationMessageReceivedEvent<*>.output() = when (messageHeader) {
    // requests
    is ForumInvitationRequest -> (messageHeader as ForumInvitationRequest).output(contactId)
    is BlogInvitationRequest -> (messageHeader as BlogInvitationRequest).output(contactId)
    is GroupInvitationRequest -> (messageHeader as GroupInvitationRequest).output(contactId)
    is IntroductionRequest -> (messageHeader as IntroductionRequest).output(contactId)
    // responses
    is ForumInvitationResponse -> (messageHeader as ForumInvitationResponse).output(contactId)
    is BlogInvitationResponse -> (messageHeader as BlogInvitationResponse).output(contactId)
    is GroupInvitationResponse -> (messageHeader as GroupInvitationResponse).output(contactId)
    is IntroductionResponse -> (messageHeader as IntroductionResponse).output(contactId)
    // unknown
    else -> throw IllegalStateException()
}

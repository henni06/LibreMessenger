@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.briarproject.briar.headless.messaging

import org.briarproject.bramble.api.contact.ContactId
import org.briarproject.briar.api.blog.BlogInvitationResponse
import org.briarproject.briar.api.forum.ForumInvitationResponse
import org.briarproject.briar.api.introduction.IntroductionResponse
import org.briarproject.briar.api.messaging.PrivateResponse
import org.briarproject.briar.api.privategroup.invitation.GroupInvitationResponse
import org.briarproject.briar.api.sharing.InvitationResponse
import org.briarproject.briar.headless.output
import javax.annotation.concurrent.Immutable

@Immutable
internal abstract class OutputPrivateResponse(header: PrivateResponse, contactId: ContactId) :
    OutputPrivateMessage(header, contactId, null) {

    val sessionId: ByteArray = header.sessionId.bytes
    val accepted = header.wasAccepted()
}

@Immutable
internal class OutputIntroductionResponse(header: IntroductionResponse, contactId: ContactId) :
    OutputPrivateResponse(header, contactId) {

    override val type = "org.briarproject.briar.api.introduction.IntroductionResponse"
    val introducedAuthor = header.introducedAuthor.output()
    val introducer = header.isIntroducer
}

@Immutable
internal class OutputInvitationResponse(header: InvitationResponse, contactId: ContactId) :
    OutputPrivateResponse(header, contactId) {

    override val type = when (header) {
        is ForumInvitationResponse -> "org.briarproject.briar.api.forum.ForumInvitationResponse"
        is BlogInvitationResponse -> "org.briarproject.briar.api.blog.BlogInvitationResponse"
        is GroupInvitationResponse -> "org.briarproject.briar.api.privategroup.invitation.GroupInvitationResponse"
        else -> throw AssertionError("Unknown InvitationResponse")
    }
    val shareableId: ByteArray = header.shareableId.bytes
}

package org.libreproject.libre.api.conversation;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.blog.BlogInvitationRequest;
import org.libreproject.libre.api.blog.BlogInvitationResponse;
import org.libreproject.libre.api.forum.ForumInvitationRequest;
import org.libreproject.libre.api.forum.ForumInvitationResponse;
import org.libreproject.libre.api.introduction.IntroductionRequest;
import org.libreproject.libre.api.introduction.IntroductionResponse;
import org.libreproject.libre.api.messaging.PrivateMessageHeader;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationRequest;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationResponse;

@NotNullByDefault
public interface ConversationMessageVisitor<T> {

	T visitPrivateMessageHeader(PrivateMessageHeader h);

	T visitBlogInvitationRequest(BlogInvitationRequest r);

	T visitBlogInvitationResponse(BlogInvitationResponse r);

	T visitForumInvitationRequest(ForumInvitationRequest r);

	T visitForumInvitationResponse(ForumInvitationResponse r);

	T visitGroupInvitationRequest(GroupInvitationRequest r);

	T visitGroupInvitationResponse(GroupInvitationResponse r);

	T visitIntroductionRequest(IntroductionRequest r);

	T visitIntroductionResponse(IntroductionResponse r);
}

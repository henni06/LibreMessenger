package org.libreproject.libre;

import org.libreproject.libre.attachment.AttachmentModule;
import org.libreproject.libre.autodelete.AutoDeleteModule;
import org.libreproject.libre.avatar.AvatarModule;
import org.libreproject.libre.blog.BlogModule;
import org.libreproject.libre.client.BriarClientModule;
import org.libreproject.libre.conversation.ConversationModule;
import org.libreproject.libre.feed.DnsModule;
import org.libreproject.libre.feed.FeedModule;
import org.libreproject.libre.forum.ForumModule;
import org.libreproject.libre.identity.IdentityModule;
import org.libreproject.libre.introduction.IntroductionModule;
import org.libreproject.libre.messaging.MessagingModule;
import org.libreproject.libre.privategroup.PrivateGroupModule;
import org.libreproject.libre.privategroup.invitation.GroupInvitationModule;
import org.libreproject.libre.sharing.SharingModule;
import org.libreproject.libre.test.TestModule;

import dagger.Module;

@Module(includes = {
		AttachmentModule.class,
		AutoDeleteModule.class,
		AvatarModule.class,
		BlogModule.class,
		BriarClientModule.class,
		ConversationModule.class,
		DnsModule.class,
		FeedModule.class,
		ForumModule.class,
		GroupInvitationModule.class,
		IdentityModule.class,
		IntroductionModule.class,
		MessagingModule.class,
		PrivateGroupModule.class,
		SharingModule.class,
		TestModule.class
})
public class BriarCoreModule {
}

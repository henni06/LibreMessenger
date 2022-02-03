package org.libreproject.libre.test;

import org.libreproject.bramble.BrambleCoreIntegrationTestEagerSingletons;
import org.libreproject.bramble.BrambleCoreModule;
import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.db.DatabaseComponent;
import org.libreproject.bramble.api.identity.AuthorFactory;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.properties.TransportPropertyManager;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.bramble.test.BrambleCoreIntegrationTestModule;
import org.libreproject.bramble.test.BrambleIntegrationTestComponent;
import org.libreproject.bramble.test.TimeTravel;
import org.libreproject.libre.api.attachment.AttachmentReader;
import org.libreproject.libre.api.autodelete.AutoDeleteManager;
import org.libreproject.libre.api.avatar.AvatarManager;
import org.libreproject.libre.api.blog.BlogFactory;
import org.libreproject.libre.api.blog.BlogManager;
import org.libreproject.libre.api.blog.BlogSharingManager;
import org.libreproject.libre.api.client.MessageTracker;
import org.libreproject.libre.api.conversation.ConversationManager;
import org.libreproject.libre.api.forum.ForumManager;
import org.libreproject.libre.api.forum.ForumSharingManager;
import org.libreproject.libre.api.introduction.IntroductionManager;
import org.libreproject.libre.api.messaging.MessagingManager;
import org.libreproject.libre.api.messaging.PrivateMessageFactory;
import org.libreproject.libre.api.privategroup.PrivateGroupManager;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationFactory;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationManager;
import org.libreproject.libre.attachment.AttachmentModule;
import org.libreproject.libre.autodelete.AutoDeleteModule;
import org.libreproject.libre.avatar.AvatarModule;
import org.libreproject.libre.blog.BlogModule;
import org.libreproject.libre.client.BriarClientModule;
import org.libreproject.libre.conversation.ConversationModule;
import org.libreproject.libre.forum.ForumModule;
import org.libreproject.libre.identity.IdentityModule;
import org.libreproject.libre.introduction.IntroductionModule;
import org.libreproject.libre.messaging.MessagingModule;
import org.libreproject.libre.privategroup.PrivateGroupModule;
import org.libreproject.libre.privategroup.invitation.GroupInvitationModule;
import org.libreproject.libre.sharing.SharingModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreIntegrationTestModule.class,
		BrambleCoreModule.class,
		AttachmentModule.class,
		AutoDeleteModule.class,
		AvatarModule.class,
		BlogModule.class,
		BriarClientModule.class,
		ConversationModule.class,
		ForumModule.class,
		GroupInvitationModule.class,
		IdentityModule.class,
		IntroductionModule.class,
		MessagingModule.class,
		PrivateGroupModule.class,
		SharingModule.class
})
public interface BriarIntegrationTestComponent
		extends BrambleIntegrationTestComponent {

	void inject(BriarIntegrationTest<BriarIntegrationTestComponent> init);

	void inject(AutoDeleteModule.EagerSingletons init);

	void inject(AvatarModule.EagerSingletons init);

	void inject(BlogModule.EagerSingletons init);

	void inject(ConversationModule.EagerSingletons init);

	void inject(ForumModule.EagerSingletons init);

	void inject(GroupInvitationModule.EagerSingletons init);

	void inject(IdentityModule.EagerSingletons init);

	void inject(IntroductionModule.EagerSingletons init);

	void inject(MessagingModule.EagerSingletons init);

	void inject(PrivateGroupModule.EagerSingletons init);

	void inject(SharingModule.EagerSingletons init);

	LifecycleManager getLifecycleManager();

	AttachmentReader getAttachmentReader();

	AvatarManager getAvatarManager();

	ContactManager getContactManager();

	ConversationManager getConversationManager();

	DatabaseComponent getDatabaseComponent();

	BlogManager getBlogManager();

	BlogSharingManager getBlogSharingManager();

	ForumSharingManager getForumSharingManager();

	ForumManager getForumManager();

	GroupInvitationManager getGroupInvitationManager();

	GroupInvitationFactory getGroupInvitationFactory();

	IntroductionManager getIntroductionManager();

	MessageTracker getMessageTracker();

	MessagingManager getMessagingManager();

	PrivateGroupManager getPrivateGroupManager();

	PrivateMessageFactory getPrivateMessageFactory();

	TransportPropertyManager getTransportPropertyManager();

	AuthorFactory getAuthorFactory();

	BlogFactory getBlogFactory();

	AutoDeleteManager getAutoDeleteManager();

	Clock getClock();

	TimeTravel getTimeTravel();

	class Helper {

		public static void injectEagerSingletons(
				BriarIntegrationTestComponent c) {
			BrambleCoreIntegrationTestEagerSingletons.Helper
					.injectEagerSingletons(c);
			c.inject(new AutoDeleteModule.EagerSingletons());
			c.inject(new AvatarModule.EagerSingletons());
			c.inject(new BlogModule.EagerSingletons());
			c.inject(new ConversationModule.EagerSingletons());
			c.inject(new ForumModule.EagerSingletons());
			c.inject(new GroupInvitationModule.EagerSingletons());
			c.inject(new IdentityModule.EagerSingletons());
			c.inject(new IntroductionModule.EagerSingletons());
			c.inject(new MessagingModule.EagerSingletons());
			c.inject(new PrivateGroupModule.EagerSingletons());
			c.inject(new SharingModule.EagerSingletons());
		}
	}
}

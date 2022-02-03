package org.libreproject.libre;

import org.libreproject.libre.autodelete.AutoDeleteModule;
import org.libreproject.libre.avatar.AvatarModule;
import org.libreproject.libre.blog.BlogModule;
import org.libreproject.libre.conversation.ConversationModule;
import org.libreproject.libre.feed.FeedModule;
import org.libreproject.libre.forum.ForumModule;
import org.libreproject.libre.identity.IdentityModule;
import org.libreproject.libre.introduction.IntroductionModule;
import org.libreproject.libre.messaging.MessagingModule;
import org.libreproject.libre.privategroup.PrivateGroupModule;
import org.libreproject.libre.privategroup.invitation.GroupInvitationModule;
import org.libreproject.libre.sharing.SharingModule;

public interface BriarCoreEagerSingletons {

	void inject(AutoDeleteModule.EagerSingletons init);

	void inject(AvatarModule.EagerSingletons init);

	void inject(BlogModule.EagerSingletons init);

	void inject(ConversationModule.EagerSingletons init);

	void inject(FeedModule.EagerSingletons init);

	void inject(ForumModule.EagerSingletons init);

	void inject(GroupInvitationModule.EagerSingletons init);

	void inject(IdentityModule.EagerSingletons init);

	void inject(IntroductionModule.EagerSingletons init);

	void inject(MessagingModule.EagerSingletons init);

	void inject(PrivateGroupModule.EagerSingletons init);

	void inject(SharingModule.EagerSingletons init);

	class Helper {

		public static void injectEagerSingletons(BriarCoreEagerSingletons c) {
			c.inject(new AutoDeleteModule.EagerSingletons());
			c.inject(new AvatarModule.EagerSingletons());
			c.inject(new BlogModule.EagerSingletons());
			c.inject(new ConversationModule.EagerSingletons());
			c.inject(new FeedModule.EagerSingletons());
			c.inject(new ForumModule.EagerSingletons());
			c.inject(new GroupInvitationModule.EagerSingletons());
			c.inject(new MessagingModule.EagerSingletons());
			c.inject(new PrivateGroupModule.EagerSingletons());
			c.inject(new SharingModule.EagerSingletons());
			c.inject(new IdentityModule.EagerSingletons());
			c.inject(new IntroductionModule.EagerSingletons());
		}
	}
}

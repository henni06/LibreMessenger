package org.libreproject.libre.introduction;

import org.libreproject.bramble.BrambleCoreModule;
import org.libreproject.bramble.test.BrambleCoreIntegrationTestModule;
import org.libreproject.libre.attachment.AttachmentModule;
import org.libreproject.libre.autodelete.AutoDeleteModule;
import org.libreproject.libre.avatar.AvatarModule;
import org.libreproject.libre.blog.BlogModule;
import org.libreproject.libre.client.BriarClientModule;
import org.libreproject.libre.conversation.ConversationModule;
import org.libreproject.libre.forum.ForumModule;
import org.libreproject.libre.identity.IdentityModule;
import org.libreproject.libre.messaging.MessagingModule;
import org.libreproject.libre.privategroup.PrivateGroupModule;
import org.libreproject.libre.privategroup.invitation.GroupInvitationModule;
import org.libreproject.libre.sharing.SharingModule;
import org.libreproject.libre.test.BriarIntegrationTestComponent;

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
interface IntroductionIntegrationTestComponent
		extends BriarIntegrationTestComponent {

	void inject(IntroductionIntegrationTest init);

	void inject(MessageEncoderParserIntegrationTest init);

	void inject(SessionEncoderParserIntegrationTest init);

	void inject(IntroductionCryptoIntegrationTest init);

	void inject(AutoDeleteIntegrationTest init);

	MessageEncoder getMessageEncoder();

	MessageParser getMessageParser();

	SessionParser getSessionParser();

	IntroductionCrypto getIntroductionCrypto();

}

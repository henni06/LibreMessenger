package org.briarproject.briar.introduction;

import org.briarproject.bramble.api.client.ClientHelper;
import org.briarproject.bramble.api.contact.ContactManager;
import org.briarproject.bramble.api.data.MetadataEncoder;
import org.briarproject.bramble.api.lifecycle.LifecycleManager;
import org.briarproject.bramble.api.sync.ValidationManager;
import org.briarproject.bramble.api.system.Clock;
import org.briarproject.briar.api.introduction.IntroductionManager;
import org.briarproject.briar.api.messaging.ConversationManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.briarproject.briar.api.introduction.IntroductionManager.CLIENT_ID;

@Module
public class IntroductionModule {

	public static class EagerSingletons {
		@Inject
		IntroductionValidator introductionValidator;
		@Inject
		IntroductionManager introductionManager;
	}

	@Provides
	@Singleton
	IntroductionValidator provideValidator(ValidationManager validationManager,
			MessageEncoder messageEncoder, MetadataEncoder metadataEncoder,
			ClientHelper clientHelper, Clock clock) {

		IntroductionValidator introductionValidator =
				new IntroductionValidator(messageEncoder, clientHelper,
						metadataEncoder, clock);
		validationManager.registerMessageValidator(CLIENT_ID,
				introductionValidator);

		return introductionValidator;
	}

	@Provides
	@Singleton
	IntroductionManager provideIntroductionManager(
			LifecycleManager lifecycleManager, ContactManager contactManager,
			ValidationManager validationManager,
			ConversationManager conversationManager,
			IntroductionManagerImpl introductionManager) {
		lifecycleManager.registerClient(introductionManager);
		contactManager.registerContactHook(introductionManager);
		validationManager.registerIncomingMessageHook(CLIENT_ID,
				introductionManager);
		conversationManager.registerConversationClient(introductionManager);

		return introductionManager;
	}

	@Provides
	MessageParser provideMessageParser(MessageParserImpl messageParser) {
		return messageParser;
	}

	@Provides
	MessageEncoder provideMessageEncoder(MessageEncoderImpl messageEncoder) {
		return messageEncoder;
	}

	@Provides
	SessionParser provideSessionParser(SessionParserImpl sessionParser) {
		return sessionParser;
	}

	@Provides
	SessionEncoder provideSessionEncoder(SessionEncoderImpl sessionEncoder) {
		return sessionEncoder;
	}

	@Provides
	IntroductionCrypto provideIntroductionCrypto(
			IntroductionCryptoImpl introductionCrypto) {
		return introductionCrypto;
	}

}

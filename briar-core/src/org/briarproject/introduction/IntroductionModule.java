package org.briarproject.introduction;

import org.briarproject.api.clients.ClientHelper;
import org.briarproject.api.clients.MessageQueueManager;
import org.briarproject.api.contact.ContactManager;
import org.briarproject.api.crypto.CryptoComponent;
import org.briarproject.api.data.MetadataEncoder;
import org.briarproject.api.db.DatabaseComponent;
import org.briarproject.api.identity.AuthorFactory;
import org.briarproject.api.introduction.IntroductionManager;
import org.briarproject.api.lifecycle.LifecycleManager;
import org.briarproject.api.properties.TransportPropertyManager;
import org.briarproject.api.system.Clock;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class IntroductionModule {

	public static class EagerSingletons {
		@Inject IntroductionManager introductionManager;
		@Inject IntroductionValidator introductionValidator;
	}

	@Provides
	@Singleton
	IntroductionValidator getValidator(MessageQueueManager messageQueueManager,
			IntroductionManager introductionManager,
			MetadataEncoder metadataEncoder, ClientHelper clientHelper,
			Clock clock) {

		IntroductionValidator introductionValidator = new IntroductionValidator(
				clientHelper, metadataEncoder, clock);

		messageQueueManager.registerMessageValidator(
				introductionManager.getClientId(),
				introductionValidator);

		return introductionValidator;
	}

	@Provides
	@Singleton
	IntroductionManager getIntroductionManager(
			LifecycleManager lifecycleManager,
			ContactManager contactManager,
			MessageQueueManager messageQueueManager,
			IntroductionManagerImpl introductionManager) {

		lifecycleManager.registerClient(introductionManager);
		contactManager.registerAddContactHook(introductionManager);
		contactManager.registerRemoveContactHook(introductionManager);
		messageQueueManager.registerIncomingMessageHook(
				introductionManager.getClientId(),
				introductionManager);

		return introductionManager;
	}
}

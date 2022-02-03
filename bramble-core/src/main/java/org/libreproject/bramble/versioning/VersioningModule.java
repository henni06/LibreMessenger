package org.libreproject.bramble.versioning;

import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.sync.validation.ValidationManager;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.bramble.api.versioning.ClientVersioningManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.libreproject.bramble.api.versioning.ClientVersioningManager.CLIENT_ID;
import static org.libreproject.bramble.api.versioning.ClientVersioningManager.MAJOR_VERSION;

@Module
public class VersioningModule {

	public static class EagerSingletons {
		@Inject
		ClientVersioningManager clientVersioningManager;
		@Inject
		ClientVersioningValidator clientVersioningValidator;
	}


	@Provides
	@Singleton
	ClientVersioningManager provideClientVersioningManager(
			ClientVersioningManagerImpl clientVersioningManager,
			LifecycleManager lifecycleManager, ContactManager contactManager,
			ValidationManager validationManager) {
		lifecycleManager.registerOpenDatabaseHook(clientVersioningManager);
		lifecycleManager.registerService(clientVersioningManager);
		contactManager.registerContactHook(clientVersioningManager);
		validationManager.registerIncomingMessageHook(CLIENT_ID, MAJOR_VERSION,
				clientVersioningManager);
		return clientVersioningManager;
	}

	@Provides
	@Singleton
	ClientVersioningValidator provideClientVersioningValidator(
			ClientHelper clientHelper, MetadataEncoder metadataEncoder,
			Clock clock, ValidationManager validationManager) {
		ClientVersioningValidator validator = new ClientVersioningValidator(
				clientHelper, metadataEncoder, clock);
		validationManager.registerMessageValidator(CLIENT_ID, MAJOR_VERSION,
				validator);
		return validator;
	}
}

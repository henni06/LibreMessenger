package org.libreproject.libre.avatar;

import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.data.BdfReaderFactory;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.sync.validation.ValidationManager;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.bramble.api.versioning.ClientVersioningManager;
import org.libreproject.libre.api.avatar.AvatarManager;
import org.libreproject.libre.api.avatar.AvatarMessageEncoder;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.libreproject.libre.api.avatar.AvatarManager.CLIENT_ID;
import static org.libreproject.libre.api.avatar.AvatarManager.MAJOR_VERSION;
import static org.libreproject.libre.api.avatar.AvatarManager.MINOR_VERSION;

@Module
public class AvatarModule {

	public static class EagerSingletons {
		@Inject
		AvatarValidator avatarValidator;
		@Inject
		AvatarManager avatarManager;
	}

	@Provides
	@Singleton
	AvatarValidator provideAvatarValidator(ValidationManager validationManager,
			BdfReaderFactory bdfReaderFactory, MetadataEncoder metadataEncoder,
			Clock clock) {
		AvatarValidator avatarValidator =
				new AvatarValidator(bdfReaderFactory, metadataEncoder, clock);
		validationManager.registerMessageValidator(CLIENT_ID, MAJOR_VERSION,
				avatarValidator);
		return avatarValidator;
	}

	@Provides
	@Singleton
	AvatarMessageEncoder provideMessageEncoder(
			AvatarMessageEncoderImpl messageEncoder) {
		return messageEncoder;
	}

	@Provides
	@Singleton
	AvatarManager provideAvatarManager(
			LifecycleManager lifecycleManager,
			ContactManager contactManager,
			ValidationManager validationManager,
			ClientVersioningManager clientVersioningManager,
			AvatarManagerImpl avatarManager) {
		lifecycleManager.registerOpenDatabaseHook(avatarManager);
		contactManager.registerContactHook(avatarManager);
		validationManager.registerIncomingMessageHook(CLIENT_ID,
				MAJOR_VERSION, avatarManager);
		clientVersioningManager.registerClient(CLIENT_ID,
				MAJOR_VERSION, MINOR_VERSION, avatarManager);
		return avatarManager;
	}

}

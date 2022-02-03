package org.libreproject.libre.privategroup;

import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.sync.validation.ValidationManager;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.libre.api.privategroup.GroupMessageFactory;
import org.libreproject.libre.api.privategroup.PrivateGroupFactory;
import org.libreproject.libre.api.privategroup.PrivateGroupManager;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.libreproject.libre.api.privategroup.PrivateGroupManager.CLIENT_ID;
import static org.libreproject.libre.api.privategroup.PrivateGroupManager.MAJOR_VERSION;

@Module
public class PrivateGroupModule {

	public static class EagerSingletons {
		@Inject
		GroupMessageValidator groupMessageValidator;
		@Inject
		PrivateGroupManager groupManager;
	}

	@Provides
	@Singleton
	PrivateGroupManager provideGroupManager(
			PrivateGroupManagerImpl groupManager,
			ValidationManager validationManager) {
		validationManager.registerIncomingMessageHook(CLIENT_ID, MAJOR_VERSION,
				groupManager);
		return groupManager;
	}

	@Provides
	PrivateGroupFactory providePrivateGroupFactory(
			PrivateGroupFactoryImpl privateGroupFactory) {
		return privateGroupFactory;
	}

	@Provides
	GroupMessageFactory provideGroupMessageFactory(
			GroupMessageFactoryImpl groupMessageFactory) {
		return groupMessageFactory;
	}

	@Provides
	@Singleton
	GroupMessageValidator provideGroupMessageValidator(
			PrivateGroupFactory privateGroupFactory,
			ClientHelper clientHelper, MetadataEncoder metadataEncoder,
			Clock clock, GroupInvitationFactory groupInvitationFactory,
			ValidationManager validationManager) {
		GroupMessageValidator validator = new GroupMessageValidator(
				privateGroupFactory, clientHelper, metadataEncoder, clock,
				groupInvitationFactory);
		validationManager.registerMessageValidator(CLIENT_ID, MAJOR_VERSION,
				validator);
		return validator;
	}

}

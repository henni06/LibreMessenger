package org.libreproject.libre.forum;

import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.sync.validation.ValidationManager;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.libre.api.forum.ForumFactory;
import org.libreproject.libre.api.forum.ForumManager;
import org.libreproject.libre.api.forum.ForumPostFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.libreproject.libre.api.forum.ForumManager.CLIENT_ID;
import static org.libreproject.libre.api.forum.ForumManager.MAJOR_VERSION;

@Module
public class ForumModule {

	public static class EagerSingletons {
		@Inject
		ForumManager forumManager;
		@Inject
		ForumPostValidator forumPostValidator;
	}

	@Provides
	@Singleton
	ForumManager provideForumManager(ForumManagerImpl forumManager,
			ValidationManager validationManager) {
		validationManager.registerIncomingMessageHook(CLIENT_ID, MAJOR_VERSION,
				forumManager);
		return forumManager;
	}

	@Provides
	ForumPostFactory provideForumPostFactory(
			ForumPostFactoryImpl forumPostFactory) {
		return forumPostFactory;
	}

	@Provides
	ForumFactory provideForumFactory(ForumFactoryImpl forumFactory) {
		return forumFactory;
	}

	@Provides
	@Singleton
	ForumPostValidator provideForumPostValidator(
			ValidationManager validationManager, ClientHelper clientHelper,
			MetadataEncoder metadataEncoder, Clock clock) {
		ForumPostValidator validator = new ForumPostValidator(clientHelper,
				metadataEncoder, clock);
		validationManager.registerMessageValidator(CLIENT_ID, MAJOR_VERSION,
				validator);
		return validator;
	}

}

package org.libreproject.libre.blog;

import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.sync.GroupFactory;
import org.libreproject.bramble.api.sync.MessageFactory;
import org.libreproject.bramble.api.sync.validation.ValidationManager;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.libre.api.blog.BlogFactory;
import org.libreproject.libre.api.blog.BlogManager;
import org.libreproject.libre.api.blog.BlogPostFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.libreproject.libre.api.blog.BlogManager.CLIENT_ID;
import static org.libreproject.libre.api.blog.BlogManager.MAJOR_VERSION;

@Module
public class BlogModule {

	public static class EagerSingletons {
		@Inject
		BlogPostValidator blogPostValidator;
		@Inject
		BlogManager blogManager;
	}

	@Provides
	@Singleton
	BlogManager provideBlogManager(BlogManagerImpl blogManager,
			LifecycleManager lifecycleManager, ContactManager contactManager,
			ValidationManager validationManager) {
		lifecycleManager.registerOpenDatabaseHook(blogManager);
		contactManager.registerContactHook(blogManager);
		validationManager.registerIncomingMessageHook(CLIENT_ID, MAJOR_VERSION,
				blogManager);
		return blogManager;
	}

	@Provides
	BlogPostFactory provideBlogPostFactory(
			BlogPostFactoryImpl blogPostFactory) {
		return blogPostFactory;
	}

	@Provides
	BlogFactory provideBlogFactory(BlogFactoryImpl blogFactory) {
		return blogFactory;
	}

	@Provides
	@Singleton
	BlogPostValidator provideBlogPostValidator(
			ValidationManager validationManager, GroupFactory groupFactory,
			MessageFactory messageFactory, BlogFactory blogFactory,
			ClientHelper clientHelper, MetadataEncoder metadataEncoder,
			Clock clock) {
		BlogPostValidator validator = new BlogPostValidator(groupFactory,
				messageFactory, blogFactory, clientHelper, metadataEncoder,
				clock);
		validationManager.registerMessageValidator(CLIENT_ID, MAJOR_VERSION,
				validator);
		return validator;
	}

}

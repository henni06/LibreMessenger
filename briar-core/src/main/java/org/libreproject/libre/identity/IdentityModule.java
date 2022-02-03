package org.libreproject.libre.identity;

import org.libreproject.libre.api.identity.AuthorManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class IdentityModule {

	public static class EagerSingletons {
		@Inject
		AuthorManager authorManager;
	}

	@Provides
	@Singleton
	AuthorManager provideAuthorManager(AuthorManagerImpl authorManager) {
		return authorManager;
	}

}

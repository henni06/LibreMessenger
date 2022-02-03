package org.libreproject.bramble.identity;

import org.libreproject.bramble.api.identity.AuthorFactory;
import org.libreproject.bramble.api.identity.IdentityManager;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class IdentityModule {

	public static class EagerSingletons {
		@Inject
		IdentityManager identityManager;
	}

	@Provides
	AuthorFactory provideAuthorFactory(AuthorFactoryImpl authorFactory) {
		return authorFactory;
	}

	@Provides
	@Singleton
	IdentityManager provideIdentityManager(LifecycleManager lifecycleManager,
			IdentityManagerImpl identityManager) {
		lifecycleManager.registerOpenDatabaseHook(identityManager);
		return identityManager;
	}
}

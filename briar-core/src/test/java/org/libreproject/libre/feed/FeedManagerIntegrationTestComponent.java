package org.libreproject.libre.feed;

import org.libreproject.bramble.BrambleCoreIntegrationTestEagerSingletons;
import org.libreproject.bramble.BrambleCoreModule;
import org.libreproject.bramble.api.identity.IdentityManager;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.test.BrambleCoreIntegrationTestModule;
import org.libreproject.bramble.test.TestSocksModule;
import org.libreproject.libre.api.blog.BlogManager;
import org.libreproject.libre.api.feed.FeedManager;
import org.libreproject.libre.avatar.AvatarModule;
import org.libreproject.libre.blog.BlogModule;
import org.libreproject.libre.client.BriarClientModule;
import org.libreproject.libre.identity.IdentityModule;
import org.libreproject.libre.test.TestDnsModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreIntegrationTestModule.class,
		BrambleCoreModule.class,
		AvatarModule.class,
		BlogModule.class,
		BriarClientModule.class,
		FeedModule.class,
		IdentityModule.class,
		TestDnsModule.class,
		TestSocksModule.class,
})
interface FeedManagerIntegrationTestComponent
		extends BrambleCoreIntegrationTestEagerSingletons {

	void inject(FeedManagerIntegrationTest testCase);

	void inject(AvatarModule.EagerSingletons init);

	void inject(BlogModule.EagerSingletons init);

	void inject(FeedModule.EagerSingletons init);

	IdentityManager getIdentityManager();

	LifecycleManager getLifecycleManager();

	FeedManager getFeedManager();

	BlogManager getBlogManager();

	class Helper {

		public static void injectEagerSingletons(
				FeedManagerIntegrationTestComponent c) {
			BrambleCoreIntegrationTestEagerSingletons.Helper
					.injectEagerSingletons(c);
			c.inject(new AvatarModule.EagerSingletons());
			c.inject(new BlogModule.EagerSingletons());
			c.inject(new FeedModule.EagerSingletons());
		}
	}
}

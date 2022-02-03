package org.libreproject.bramble.plugin;

import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.plugin.BackoffFactory;
import org.libreproject.bramble.api.plugin.PluginConfig;
import org.libreproject.bramble.api.plugin.PluginManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class PluginModule {

	public static class EagerSingletons {
		@Inject
		PluginManager pluginManager;
		@Inject
		Poller poller;
	}

	@Provides
	BackoffFactory provideBackoffFactory() {
		return new BackoffFactoryImpl();
	}

	@Provides
	@Singleton
	PluginManager providePluginManager(LifecycleManager lifecycleManager,
			PluginManagerImpl pluginManager) {
		lifecycleManager.registerService(pluginManager);
		return pluginManager;
	}

	@Provides
	@Singleton
	Poller providePoller(PluginConfig config, EventBus eventBus,
			PollerImpl poller) {
		if (config.shouldPoll()) eventBus.addListener(poller);
		return poller;
	}
}

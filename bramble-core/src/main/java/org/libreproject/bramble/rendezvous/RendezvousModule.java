package org.libreproject.bramble.rendezvous;

import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.rendezvous.RendezvousPoller;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class RendezvousModule {

	public static class EagerSingletons {
		@Inject
		RendezvousPoller rendezvousPoller;
	}

	@Provides
	RendezvousCrypto provideRendezvousCrypto(
			RendezvousCryptoImpl rendezvousCrypto) {
		return rendezvousCrypto;
	}

	@Provides
	@Singleton
	RendezvousPoller provideRendezvousPoller(LifecycleManager lifecycleManager,
			EventBus eventBus, RendezvousPollerImpl rendezvousPoller) {
		lifecycleManager.registerService(rendezvousPoller);
		eventBus.addListener(rendezvousPoller);
		return rendezvousPoller;
	}
}

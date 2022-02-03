package org.libreproject.bramble.event;

import org.libreproject.bramble.api.event.EventBus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class EventModule {

	@Provides
	@Singleton
	EventBus provideEventBus(EventBusImpl eventBus) {
		return eventBus;
	}
}

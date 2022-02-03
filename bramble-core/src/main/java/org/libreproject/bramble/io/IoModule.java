package org.libreproject.bramble.io;

import org.libreproject.bramble.api.io.TimeoutMonitor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class IoModule {

	@Provides
	@Singleton
	TimeoutMonitor provideTimeoutMonitor(TimeoutMonitorImpl timeoutMonitor) {
		return timeoutMonitor;
	}
}

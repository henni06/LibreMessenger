package org.libreproject.bramble.system;

import org.libreproject.bramble.api.system.LocationUtils;
import org.libreproject.bramble.api.system.ResourceProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class JavaSystemModule {

	@Provides
	@Singleton
	LocationUtils provideLocationUtils(JavaLocationUtils locationUtils) {
		return locationUtils;
	}

	@Provides
	@Singleton
	ResourceProvider provideResourceProvider(JavaResourceProvider provider) {
		return provider;
	}
}

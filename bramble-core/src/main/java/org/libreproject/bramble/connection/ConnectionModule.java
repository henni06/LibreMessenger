package org.libreproject.bramble.connection;

import org.libreproject.bramble.api.connection.ConnectionManager;
import org.libreproject.bramble.api.connection.ConnectionRegistry;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ConnectionModule {

	@Provides
	ConnectionManager provideConnectionManager(
			ConnectionManagerImpl connectionManager) {
		return connectionManager;
	}

	@Provides
	@Singleton
	ConnectionRegistry provideConnectionRegistry(
			ConnectionRegistryImpl connectionRegistry) {
		return connectionRegistry;
	}
}

package org.libreproject.bramble.network;

import org.libreproject.bramble.api.network.NetworkManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class JavaNetworkModule {

	@Provides
	@Singleton
	NetworkManager provideNetworkManager(JavaNetworkManager networkManager) {
		return networkManager;
	}
}

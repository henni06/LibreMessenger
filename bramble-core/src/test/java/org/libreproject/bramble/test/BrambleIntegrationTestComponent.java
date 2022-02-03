package org.libreproject.bramble.test;

import org.libreproject.bramble.BrambleCoreIntegrationTestEagerSingletons;
import org.libreproject.bramble.BrambleCoreModule;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.connection.ConnectionManager;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.identity.IdentityManager;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreIntegrationTestModule.class,
		BrambleCoreModule.class
})
public interface BrambleIntegrationTestComponent
		extends BrambleCoreIntegrationTestEagerSingletons {

	IdentityManager getIdentityManager();

	EventBus getEventBus();

	ConnectionManager getConnectionManager();

	ClientHelper getClientHelper();

}

package org.libreproject.bramble.contact;

import org.libreproject.bramble.BrambleCoreIntegrationTestEagerSingletons;
import org.libreproject.bramble.BrambleCoreModule;
import org.libreproject.bramble.api.connection.ConnectionManager;
import org.libreproject.bramble.api.contact.ContactExchangeManager;
import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.identity.IdentityManager;
import org.libreproject.bramble.api.lifecycle.IoExecutor;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.test.BrambleCoreIntegrationTestModule;

import java.util.concurrent.Executor;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreIntegrationTestModule.class,
		BrambleCoreModule.class
})
interface ContactExchangeIntegrationTestComponent
		extends BrambleCoreIntegrationTestEagerSingletons {

	ConnectionManager getConnectionManager();

	ContactExchangeManager getContactExchangeManager();

	ContactManager getContactManager();

	EventBus getEventBus();

	IdentityManager getIdentityManager();

	@IoExecutor
	Executor getIoExecutor();

	LifecycleManager getLifecycleManager();
}

package org.libreproject.bramble.transport.agreement;

import org.libreproject.bramble.BrambleCoreModule;
import org.libreproject.bramble.api.client.ContactGroupFactory;
import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.db.DatabaseComponent;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.properties.TransportPropertyManager;
import org.libreproject.bramble.api.transport.KeyManager;
import org.libreproject.bramble.test.BrambleCoreIntegrationTestModule;
import org.libreproject.bramble.test.BrambleIntegrationTestComponent;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreIntegrationTestModule.class,
		BrambleCoreModule.class
})
interface TransportKeyAgreementTestComponent
		extends BrambleIntegrationTestComponent {

	KeyManager getKeyManager();

	TransportKeyAgreementManagerImpl getTransportKeyAgreementManager();

	ContactManager getContactManager();

	LifecycleManager getLifecycleManager();

	ContactGroupFactory getContactGroupFactory();

	SessionParser getSessionParser();

	TransportPropertyManager getTransportPropertyManager();

	DatabaseComponent getDatabaseComponent();
}

package org.libreproject.libre.messaging;

import org.libreproject.bramble.BrambleCoreIntegrationTestEagerSingletons;
import org.libreproject.bramble.BrambleCoreModule;
import org.libreproject.bramble.api.connection.ConnectionManager;
import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.identity.IdentityManager;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.test.BrambleCoreIntegrationTestModule;
import org.libreproject.libre.api.messaging.MessagingManager;
import org.libreproject.libre.api.messaging.PrivateMessageFactory;
import org.libreproject.libre.autodelete.AutoDeleteModule;
import org.libreproject.libre.client.BriarClientModule;
import org.libreproject.libre.conversation.ConversationModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		AutoDeleteModule.class,
		BrambleCoreIntegrationTestModule.class,
		BrambleCoreModule.class,
		BriarClientModule.class,
		ConversationModule.class,
		MessagingModule.class
})
interface SimplexMessagingIntegrationTestComponent
		extends BrambleCoreIntegrationTestEagerSingletons {

	void inject(MessagingModule.EagerSingletons init);

	LifecycleManager getLifecycleManager();

	IdentityManager getIdentityManager();

	ContactManager getContactManager();

	MessagingManager getMessagingManager();

	PrivateMessageFactory getPrivateMessageFactory();

	EventBus getEventBus();

	ConnectionManager getConnectionManager();

	class Helper {

		public static void injectEagerSingletons(
				SimplexMessagingIntegrationTestComponent c) {
			BrambleCoreIntegrationTestEagerSingletons.Helper
					.injectEagerSingletons(c);
			c.inject(new MessagingModule.EagerSingletons());
		}
	}
}

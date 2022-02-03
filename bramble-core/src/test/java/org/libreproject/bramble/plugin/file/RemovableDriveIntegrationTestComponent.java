package org.libreproject.bramble.plugin.file;

import org.libreproject.bramble.BrambleCoreEagerSingletons;
import org.libreproject.bramble.BrambleCoreModule;
import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.identity.IdentityManager;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.plugin.file.RemovableDriveManager;
import org.libreproject.bramble.battery.DefaultBatteryManagerModule;
import org.libreproject.bramble.event.DefaultEventExecutorModule;
import org.libreproject.bramble.system.DefaultWakefulIoExecutorModule;
import org.libreproject.bramble.system.TimeTravelModule;
import org.libreproject.bramble.test.TestDatabaseConfigModule;
import org.libreproject.bramble.test.TestFeatureFlagModule;
import org.libreproject.bramble.test.TestSecureRandomModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreModule.class,
		DefaultBatteryManagerModule.class,
		DefaultEventExecutorModule.class,
		DefaultWakefulIoExecutorModule.class,
		TestDatabaseConfigModule.class,
		TestFeatureFlagModule.class,
		RemovableDriveIntegrationTestModule.class,
		RemovableDriveModule.class,
		TestSecureRandomModule.class,
		TimeTravelModule.class
})
interface RemovableDriveIntegrationTestComponent
		extends BrambleCoreEagerSingletons {

	ContactManager getContactManager();

	EventBus getEventBus();

	IdentityManager getIdentityManager();

	LifecycleManager getLifecycleManager();

	RemovableDriveManager getRemovableDriveManager();

	class Helper {

		public static void injectEagerSingletons(
				RemovableDriveIntegrationTestComponent c) {
			BrambleCoreEagerSingletons.Helper.injectEagerSingletons(c);
		}
	}
}

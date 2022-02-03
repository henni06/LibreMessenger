package org.libreproject.bramble.test;

import org.libreproject.bramble.battery.DefaultBatteryManagerModule;
import org.libreproject.bramble.event.DefaultEventExecutorModule;
import org.libreproject.bramble.system.DefaultWakefulIoExecutorModule;
import org.libreproject.bramble.system.TimeTravelModule;

import dagger.Module;

@Module(includes = {
		DefaultBatteryManagerModule.class,
		DefaultEventExecutorModule.class,
		DefaultWakefulIoExecutorModule.class,
		TestDatabaseConfigModule.class,
		TestFeatureFlagModule.class,
		TestPluginConfigModule.class,
		TestSecureRandomModule.class,
		TimeTravelModule.class
})
public class BrambleCoreIntegrationTestModule {

}

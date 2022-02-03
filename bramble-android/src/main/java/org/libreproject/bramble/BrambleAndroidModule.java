package org.libreproject.bramble;

import org.libreproject.bramble.battery.AndroidBatteryModule;
import org.libreproject.bramble.network.AndroidNetworkModule;
import org.libreproject.bramble.plugin.tor.CircumventionModule;
import org.libreproject.bramble.reporting.ReportingModule;
import org.libreproject.bramble.socks.SocksModule;
import org.libreproject.bramble.system.AndroidSystemModule;
import org.libreproject.bramble.system.AndroidTaskSchedulerModule;
import org.libreproject.bramble.system.AndroidWakefulIoExecutorModule;

import dagger.Module;

@Module(includes = {
		AndroidBatteryModule.class,
		AndroidNetworkModule.class,
		AndroidSystemModule.class,
		AndroidTaskSchedulerModule.class,
		AndroidWakefulIoExecutorModule.class,
		CircumventionModule.class,
		ReportingModule.class,
		SocksModule.class
})
public class BrambleAndroidModule {
}

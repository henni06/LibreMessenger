package org.libreproject.bramble.lifecycle;

import org.libreproject.bramble.api.lifecycle.ShutdownManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.libreproject.bramble.util.OsUtils.isWindows;

@Module
public class DesktopLifecycleModule extends LifecycleModule {

	@Provides
	@Singleton
	ShutdownManager provideDesktopShutdownManager() {
		if (isWindows()) return new WindowsShutdownManagerImpl();
		else return new ShutdownManagerImpl();
	}
}

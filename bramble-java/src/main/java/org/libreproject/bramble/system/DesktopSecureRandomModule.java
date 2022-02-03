package org.libreproject.bramble.system;

import org.libreproject.bramble.api.system.SecureRandomProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.libreproject.bramble.util.OsUtils.isLinux;
import static org.libreproject.bramble.util.OsUtils.isMac;

@Module
public class DesktopSecureRandomModule {

	@Provides
	@Singleton
	SecureRandomProvider provideSecureRandomProvider() {
		if (isLinux() || isMac())
			return new UnixSecureRandomProvider();
		// TODO: Create a secure random provider for Windows
		throw new UnsupportedOperationException();
	}
}

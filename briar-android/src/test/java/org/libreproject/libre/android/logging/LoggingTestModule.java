package org.libreproject.libre.android.logging;

import org.libreproject.bramble.api.crypto.PublicKey;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.reporting.DevConfig;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
class LoggingTestModule {

	private final File logFile;

	LoggingTestModule(File logFile) {
		this.logFile = logFile;
	}

	@Provides
	@Singleton
	DevConfig provideDevConfig() {
		@NotNullByDefault
		DevConfig devConfig = new DevConfig() {
			@Override
			public PublicKey getDevPublicKey() {
				throw new UnsupportedOperationException();

			}

			@Override
			public String getDevOnionAddress() {
				throw new UnsupportedOperationException();
			}

			@Override
			public File getReportDir() {
				throw new UnsupportedOperationException();
			}

			@Override
			public File getLogcatFile() {
				return logFile;
			}
		};
		return devConfig;
	}

}

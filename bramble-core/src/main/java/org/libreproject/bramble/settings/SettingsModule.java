package org.libreproject.bramble.settings;

import org.libreproject.bramble.api.db.DatabaseComponent;
import org.libreproject.bramble.api.settings.SettingsManager;

import dagger.Module;
import dagger.Provides;

@Module
public class SettingsModule {

	@Provides
	SettingsManager provideSettingsManager(DatabaseComponent db) {
		return new SettingsManagerImpl(db);
	}

}

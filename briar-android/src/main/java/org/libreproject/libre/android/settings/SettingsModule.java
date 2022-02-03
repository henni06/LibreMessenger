package org.libreproject.libre.android.settings;

import org.libreproject.libre.android.viewmodel.ViewModelKey;

import androidx.lifecycle.ViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class SettingsModule {

	@Binds
	@IntoMap
	@ViewModelKey(SettingsViewModel.class)
	abstract ViewModel bindSettingsViewModel(
			SettingsViewModel settingsViewModel);

}

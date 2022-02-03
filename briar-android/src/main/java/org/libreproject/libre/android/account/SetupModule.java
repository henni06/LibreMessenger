package org.libreproject.libre.android.account;

import org.libreproject.libre.android.viewmodel.ViewModelKey;

import androidx.lifecycle.ViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class SetupModule {

	@Binds
	@IntoMap
	@ViewModelKey(SetupViewModel.class)
	abstract ViewModel bindSetupViewModel(
			SetupViewModel setupViewModel);
}

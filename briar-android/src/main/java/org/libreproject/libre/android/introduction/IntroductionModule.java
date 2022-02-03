package org.libreproject.libre.android.introduction;

import org.libreproject.libre.android.viewmodel.ViewModelKey;

import androidx.lifecycle.ViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class IntroductionModule {

	@Binds
	@IntoMap
	@ViewModelKey(IntroductionViewModel.class)
	abstract ViewModel bindIntroductionViewModel(
			IntroductionViewModel introductionViewModel);

}

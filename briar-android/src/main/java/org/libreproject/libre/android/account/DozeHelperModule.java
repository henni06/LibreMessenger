package org.libreproject.libre.android.account;

import dagger.Module;
import dagger.Provides;

@Module
public class DozeHelperModule {

	@Provides
	DozeHelper provideDozeHelper() {
		return new DozeHelperImpl();
	}
}

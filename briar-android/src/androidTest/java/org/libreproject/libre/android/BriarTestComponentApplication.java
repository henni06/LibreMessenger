package org.libreproject.libre.android;

import org.libreproject.bramble.BrambleAndroidEagerSingletons;
import org.libreproject.bramble.BrambleCoreEagerSingletons;
import org.libreproject.libre.BriarCoreEagerSingletons;

public class BriarTestComponentApplication extends BriarApplicationImpl {

	@Override
	protected AndroidComponent createApplicationComponent() {
		AndroidComponent component = DaggerBriarUiTestComponent.builder()
				.appModule(new AppModule(this)).build();
		// We need to load the eager singletons directly after making the
		// dependency graphs
		BrambleCoreEagerSingletons.Helper.injectEagerSingletons(component);
		BrambleAndroidEagerSingletons.Helper.injectEagerSingletons(component);
		BriarCoreEagerSingletons.Helper.injectEagerSingletons(component);
		AndroidEagerSingletons.Helper.injectEagerSingletons(component);
		return component;
	}

	@Override
	public boolean isInstrumentationTest() {
		return true;
	}

}

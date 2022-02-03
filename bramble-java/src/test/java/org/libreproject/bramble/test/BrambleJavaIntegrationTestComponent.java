package org.libreproject.bramble.test;

import org.libreproject.bramble.BrambleCoreIntegrationTestEagerSingletons;
import org.libreproject.bramble.BrambleCoreModule;
import org.libreproject.bramble.BrambleJavaModule;
import org.libreproject.bramble.plugin.tor.BridgeTest;
import org.libreproject.bramble.plugin.tor.CircumventionProvider;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreIntegrationTestModule.class,
		BrambleCoreModule.class,
		BrambleJavaModule.class
})
public interface BrambleJavaIntegrationTestComponent
		extends BrambleCoreIntegrationTestEagerSingletons {

	void inject(BridgeTest init);

	CircumventionProvider getCircumventionProvider();
}

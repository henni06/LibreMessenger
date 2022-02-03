package org.libreproject.bramble.sync;

import org.libreproject.bramble.BrambleCoreIntegrationTestEagerSingletons;
import org.libreproject.bramble.BrambleCoreModule;
import org.libreproject.bramble.test.BrambleCoreIntegrationTestModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreIntegrationTestModule.class,
		BrambleCoreModule.class
})
interface SyncIntegrationTestComponent extends
		BrambleCoreIntegrationTestEagerSingletons {

	void inject(SyncIntegrationTest testCase);
}

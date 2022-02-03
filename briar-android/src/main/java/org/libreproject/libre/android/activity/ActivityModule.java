package org.libreproject.libre.android.activity;

import android.app.Activity;

import org.libreproject.libre.android.controller.BriarController;
import org.libreproject.libre.android.controller.BriarControllerImpl;
import org.libreproject.libre.android.controller.DbController;
import org.libreproject.libre.android.controller.DbControllerImpl;

import dagger.Module;
import dagger.Provides;

import static org.libreproject.libre.android.BriarService.BriarServiceConnection;

@Module
public class ActivityModule {

	private final BaseActivity activity;

	public ActivityModule(BaseActivity activity) {
		this.activity = activity;
	}

	@ActivityScope
	@Provides
	BaseActivity provideBaseActivity() {
		return activity;
	}

	@ActivityScope
	@Provides
	Activity provideActivity() {
		return activity;
	}

	@ActivityScope
	@Provides
	protected BriarController provideBriarController(
			BriarControllerImpl briarController) {
		activity.addLifecycleController(briarController);
		return briarController;
	}

	@ActivityScope
	@Provides
	DbController provideDBController(DbControllerImpl dbController) {
		return dbController;
	}

	@ActivityScope
	@Provides
	BriarServiceConnection provideBriarServiceConnection() {
		return new BriarServiceConnection();
	}
}

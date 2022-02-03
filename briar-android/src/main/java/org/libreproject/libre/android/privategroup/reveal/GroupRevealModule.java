package org.libreproject.libre.android.privategroup.reveal;

import org.libreproject.libre.android.activity.ActivityScope;

import dagger.Module;
import dagger.Provides;

@Module
public class GroupRevealModule {

	@ActivityScope
	@Provides
	RevealContactsController provideRevealContactsController(
			RevealContactsControllerImpl revealContactsController) {
		return revealContactsController;
	}
}

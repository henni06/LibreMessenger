package org.briarproject.briar.android;

import org.briarproject.bramble.BrambleAndroidModule;
import org.briarproject.bramble.BrambleCoreModule;
import org.briarproject.bramble.account.BriarAccountModule;
import org.briarproject.briar.BriarCoreModule;
import org.briarproject.briar.android.attachment.AttachmentModule;
import org.briarproject.briar.android.attachment.media.MediaModule;
import org.briarproject.briar.android.navdrawer.NavDrawerActivityTest;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		AppModule.class,
		AttachmentModule.class,
		MediaModule.class,
		BriarCoreModule.class,
		BrambleAndroidModule.class,
		BriarAccountModule.class,
		BrambleCoreModule.class
})
public interface BriarUiTestComponent extends AndroidComponent {

	void inject(NavDrawerActivityTest test);

}

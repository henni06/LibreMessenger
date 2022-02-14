package org.libreproject.libre.android;

import org.libreproject.bramble.BrambleAndroidModule;
import org.libreproject.bramble.BrambleCoreModule;
import org.libreproject.bramble.account.LibreAccountModule;
import org.libreproject.bramble.plugin.file.RemovableDriveModule;
import org.libreproject.bramble.system.ClockModule;
import org.libreproject.libre.BriarCoreModule;
import org.libreproject.libre.android.account.SignInTestCreateAccount;
import org.libreproject.libre.android.account.SignInTestSignIn;
import org.libreproject.libre.android.attachment.AttachmentModule;
import org.libreproject.libre.android.attachment.media.MediaModule;
import org.libreproject.libre.android.navdrawer.NavDrawerActivityTest;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		AppModule.class,
		AttachmentModule.class,
		ClockModule.class,
		MediaModule.class,
		RemovableDriveModule.class,
		BriarCoreModule.class,
		BrambleAndroidModule.class,
		LibreAccountModule.class,
		BrambleCoreModule.class
})
public interface BriarUiTestComponent extends AndroidComponent {

	void inject(NavDrawerActivityTest test);

	void inject(SignInTestCreateAccount test);

	void inject(SignInTestSignIn test);

}

package org.libreproject.libre.android;

import org.libreproject.bramble.BrambleAndroidModule;
import org.libreproject.bramble.BrambleCoreModule;
import org.libreproject.bramble.account.BriarAccountModule;
import org.libreproject.bramble.plugin.file.RemovableDriveModule;
import org.libreproject.bramble.system.ClockModule;
import org.libreproject.libre.BriarCoreModule;
import org.libreproject.libre.android.attachment.AttachmentModule;
import org.libreproject.libre.android.attachment.media.MediaModule;
import org.libreproject.libre.android.conversation.ConversationActivityScreenshotTest;
import org.libreproject.libre.android.settings.SettingsActivityScreenshotTest;

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
		BriarAccountModule.class,
		BrambleCoreModule.class
})
public interface BriarUiTestComponent extends AndroidComponent {

	void inject(SetupDataTest test);

	void inject(ConversationActivityScreenshotTest test);

	void inject(SettingsActivityScreenshotTest test);

	void inject(PromoVideoTest test);

}

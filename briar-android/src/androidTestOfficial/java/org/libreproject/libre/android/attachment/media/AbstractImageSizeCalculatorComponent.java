package org.libreproject.libre.android.attachment.media;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		MediaModule.class
})
interface AbstractImageSizeCalculatorComponent {

	void inject(AbstractImageSizeCalculatorTest test);

}

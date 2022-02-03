package org.libreproject.libre.android.attachment;

import org.libreproject.libre.android.attachment.media.MediaModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		MediaModule.class
})
interface AbstractAttachmentRetrieverComponent {

	void inject(AttachmentRetrieverIntegrationTest test);

}

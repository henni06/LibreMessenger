package org.libreproject.libre.android.conversation.glide;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.android.LibreApplication;
import org.libreproject.libre.api.attachment.AttachmentHeader;

import java.io.InputStream;

@NotNullByDefault
class BriarModelLoaderFactory
		implements ModelLoaderFactory<AttachmentHeader, InputStream> {

	private final LibreApplication app;

	BriarModelLoaderFactory(LibreApplication app) {
		this.app = app;
	}

	@Override
	public ModelLoader<AttachmentHeader, InputStream> build(
			MultiModelLoaderFactory multiFactory) {
		return new BriarModelLoader(app);
	}

	@Override
	public void teardown() {
		// noop
	}

}

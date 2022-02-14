package org.libreproject.libre.android.conversation.glide;


import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.libre.android.LibreApplication;
import org.libreproject.libre.api.attachment.AttachmentHeader;

import java.io.InputStream;

import javax.inject.Inject;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public final class BriarModelLoader
		implements ModelLoader<AttachmentHeader, InputStream> {

	@Inject
	BriarDataFetcherFactory dataFetcherFactory;

	BriarModelLoader(LibreApplication app) {
		app.getApplicationComponent().inject(this);
	}

	@Override
	public LoadData<InputStream> buildLoadData(AttachmentHeader model,
			int width, int height, Options options) {
		ObjectKey key = new ObjectKey(model.getMessageId());
		BriarDataFetcher dataFetcher =
				dataFetcherFactory.createBriarDataFetcher(model);
		return new LoadData<>(key, dataFetcher);
	}

	@Override
	public boolean handles(AttachmentHeader model) {
		return true;
	}
}

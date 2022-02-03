package org.libreproject.libre.android.conversation.glide;

import org.libreproject.bramble.api.db.DatabaseExecutor;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.attachment.AttachmentHeader;
import org.libreproject.libre.api.attachment.AttachmentReader;

import java.util.concurrent.Executor;

import javax.inject.Inject;

@NotNullByDefault
public class BriarDataFetcherFactory {

	private final AttachmentReader attachmentReader;
	@DatabaseExecutor
	private final Executor dbExecutor;

	@Inject
	public BriarDataFetcherFactory(AttachmentReader attachmentReader,
			@DatabaseExecutor Executor dbExecutor) {
		this.attachmentReader = attachmentReader;
		this.dbExecutor = dbExecutor;
	}

	BriarDataFetcher createBriarDataFetcher(AttachmentHeader model) {
		return new BriarDataFetcher(attachmentReader, dbExecutor, model);
	}

}

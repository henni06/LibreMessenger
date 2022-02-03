package org.libreproject.libre.android.attachment;

import android.content.ContentResolver;
import android.net.Uri;

import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.lifecycle.IoExecutor;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.android.attachment.media.ImageCompressor;
import org.libreproject.libre.api.attachment.AttachmentHeader;
import org.libreproject.libre.api.messaging.MessagingManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Logger;

import androidx.annotation.Nullable;

import static java.util.Arrays.asList;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.libreproject.bramble.util.AndroidUtils.getSupportedImageContentTypes;
import static org.libreproject.bramble.util.IoUtils.tryToClose;
import static org.libreproject.bramble.util.LogUtils.logDuration;
import static org.libreproject.bramble.util.LogUtils.logException;
import static org.libreproject.bramble.util.LogUtils.now;

@NotNullByDefault
class AttachmentCreationTask {

	private static final Logger LOG =
			getLogger(AttachmentCreationTask.class.getName());

	private final MessagingManager messagingManager;
	private final ContentResolver contentResolver;
	private final ImageCompressor imageCompressor;
	private final GroupId groupId;
	private final Collection<Uri> uris;
	private final boolean needsSize;
	@Nullable
	private volatile AttachmentCreator attachmentCreator;

	private volatile boolean canceled = false;

	AttachmentCreationTask(MessagingManager messagingManager,
			ContentResolver contentResolver,
			AttachmentCreator attachmentCreator,
			ImageCompressor imageCompressor,
			GroupId groupId, Collection<Uri> uris, boolean needsSize) {
		this.messagingManager = messagingManager;
		this.contentResolver = contentResolver;
		this.imageCompressor = imageCompressor;
		this.groupId = groupId;
		this.uris = uris;
		this.needsSize = needsSize;
		this.attachmentCreator = attachmentCreator;
	}

	void cancel() {
		canceled = true;
		attachmentCreator = null;
	}

	@IoExecutor
	void storeAttachments() {
		for (Uri uri : uris) processUri(uri);
		AttachmentCreator attachmentCreator = this.attachmentCreator;
		if (!canceled && attachmentCreator != null)
			attachmentCreator.onAttachmentCreationFinished();
		this.attachmentCreator = null;
	}

	@IoExecutor
	private void processUri(Uri uri) {
		if (canceled) return;
		try {
			AttachmentHeader h = storeAttachment(uri);
			AttachmentCreator attachmentCreator = this.attachmentCreator;
			if (attachmentCreator != null) {
				attachmentCreator.onAttachmentHeaderReceived(uri, h, needsSize);
			}
		} catch (DbException | IOException e) {
			logException(LOG, WARNING, e);
			AttachmentCreator attachmentCreator = this.attachmentCreator;
			if (attachmentCreator != null) {
				attachmentCreator.onAttachmentError(uri, e);
			}
			canceled = true;
		}
	}

	@IoExecutor
	private AttachmentHeader storeAttachment(Uri uri)
			throws IOException, DbException {
		long start = now();
		String contentType = contentResolver.getType(uri);
		if (contentType == null) throw new IOException("null content type");
		if (!asList(getSupportedImageContentTypes()).contains(contentType)) {
			throw new UnsupportedMimeTypeException(contentType, uri);
		}
		InputStream is = contentResolver.openInputStream(uri);
		if (is == null) throw new IOException();
		is = imageCompressor
				.compressImage(is, contentType);
		long timestamp = System.currentTimeMillis();
		AttachmentHeader h = messagingManager
				.addLocalAttachment(groupId, timestamp,
						ImageCompressor.MIME_TYPE, is);
		tryToClose(is, LOG, WARNING);
		logDuration(LOG, "Storing attachment", start);
		return h;
	}

}

package org.libreproject.libre.android.attachment;

import android.net.Uri;

import org.libreproject.bramble.api.lifecycle.IoExecutor;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.attachment.AttachmentHeader;

import java.util.Collection;
import java.util.List;

import androidx.annotation.UiThread;
import androidx.lifecycle.LiveData;

@NotNullByDefault
public interface AttachmentCreator {

	@UiThread
	LiveData<AttachmentResult> storeAttachments(LiveData<GroupId> groupId,
			Collection<Uri> newUris);

	/**
	 * This should be only called after configuration changes.
	 * In this case we should not create new attachments.
	 * They are already being created and returned by the existing LiveData.
	 */
	@UiThread
	LiveData<AttachmentResult> getLiveAttachments();

	@UiThread
	List<AttachmentHeader> getAttachmentHeadersForSending();

	/**
	 * Marks the attachments as sent and adds the items to the cache for display
	 *
	 * @param id The MessageId of the sent message.
	 */
	@UiThread
	void onAttachmentsSent(MessageId id);

	/**
	 * Needs to be called when created attachments will not be sent anymore.
	 */
	@UiThread
	void cancel();

	@UiThread
	void deleteUnsentAttachments();

	@IoExecutor
	void onAttachmentHeaderReceived(Uri uri, AttachmentHeader h,
			boolean needsSize);

	@IoExecutor
	void onAttachmentError(Uri uri, Throwable t);

	@IoExecutor
	void onAttachmentCreationFinished();
}
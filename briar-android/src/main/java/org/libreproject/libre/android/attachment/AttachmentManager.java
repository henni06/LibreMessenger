package org.libreproject.libre.android.attachment;

import android.net.Uri;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.attachment.AttachmentHeader;

import java.util.Collection;
import java.util.List;

import androidx.annotation.UiThread;
import androidx.lifecycle.LiveData;

@UiThread
@NotNullByDefault
public interface AttachmentManager {

	LiveData<AttachmentResult> storeAttachments(Collection<Uri> uri,
			boolean restart);

	List<AttachmentHeader> getAttachmentHeadersForSending();

	void cancel();

}

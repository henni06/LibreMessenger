package org.briarproject.briar.android.conversation;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.briar.android.attachment.AttachmentItem;
import org.briarproject.briar.api.messaging.PrivateMessageHeader;

import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import androidx.annotation.LayoutRes;

@NotThreadSafe
@NotNullByDefault
class ConversationMessageItem extends ConversationItem {

	private List<AttachmentItem> attachments;

	ConversationMessageItem(@LayoutRes int layoutRes, PrivateMessageHeader h,
			List<AttachmentItem> attachments) {
		super(layoutRes, h);
		this.attachments = attachments;
	}

	List<AttachmentItem> getAttachments() {
		return attachments;
	}

	void setAttachments(List<AttachmentItem> attachments) {
		this.attachments = attachments;
	}

}

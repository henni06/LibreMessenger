package org.libreproject.libre.android.conversation;

import android.view.View;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.android.attachment.AttachmentItem;

import androidx.annotation.UiThread;

@UiThread
@NotNullByDefault
interface ConversationListener {

	void respondToRequest(ConversationRequestItem item, boolean accept);

	void openRequestedShareable(ConversationRequestItem item);

	void onAttachmentClicked(View view, ConversationMessageItem messageItem,
			AttachmentItem attachmentItem);

	void onSpeechAttachmentClicked(ConversationMessageViewHolder viewHolder, ConversationMessageItem messageItem,
			AttachmentItem attachmentItem,boolean play);

	void onSpeechStopped();

	void onAutoDeleteTimerNoticeClicked();

}

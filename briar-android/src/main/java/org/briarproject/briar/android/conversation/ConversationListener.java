package org.briarproject.briar.android.conversation;

import android.support.annotation.UiThread;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;

@UiThread
@NotNullByDefault
interface ConversationListener {

	void onItemVisible(ConversationItem item);

	void respondToRequest(ConversationRequestItem item, boolean accept);

	void openRequestedShareable(ConversationRequestItem item);

}

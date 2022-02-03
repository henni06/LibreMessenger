package org.libreproject.libre.android.conversation;

import android.view.View;
import android.widget.ImageView;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.R;

import androidx.annotation.UiThread;

@UiThread
@NotNullByDefault
class OutItemViewHolder {

	private final ImageView status;

	OutItemViewHolder(View v) {
		status = v.findViewById(R.id.status);
	}

	void bind(ConversationItem item) {
		int res;
		if (item.isSeen()) {
			res = R.drawable.message_delivered;
		} else if (item.isSent()) {
			res = R.drawable.message_sent;
		} else {
			res = R.drawable.message_stored;
		}
		status.setImageResource(res);
	}

}

package org.briarproject.briar.android.contact;

import android.support.annotation.UiThread;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.contact.ConversationAdapter.ConversationListener;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.briarproject.briar.android.contact.ConversationRequestItem.RequestType.INTRODUCTION;

@UiThread
@NotNullByDefault
class ConversationRequestViewHolder extends ConversationNoticeInViewHolder {

	private final Button acceptButton;
	private final Button declineButton;

	ConversationRequestViewHolder(View v) {
		super(v);
		acceptButton = (Button) v.findViewById(R.id.acceptButton);
		declineButton = (Button) v.findViewById(R.id.declineButton);
	}

	void bind(ConversationItem conversationItem,
			final ConversationListener listener) {
		super.bind(conversationItem);

		final ConversationRequestItem item =
				(ConversationRequestItem) conversationItem;

		if (item.getRequestType() == INTRODUCTION && item.wasAnswered()) {
			acceptButton.setVisibility(GONE);
			declineButton.setVisibility(GONE);
		} else if (item.wasAnswered()) {
			acceptButton.setVisibility(VISIBLE);
			acceptButton.setText(R.string.open);
			acceptButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					item.setAnswered(true);
					listener.open(item);
				}
			});
			declineButton.setVisibility(GONE);
		} else {
			acceptButton.setVisibility(VISIBLE);
			acceptButton.setText(R.string.accept);
			acceptButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					item.setAnswered(true);
					listener.respondToRequest(item, true);
				}
			});
			declineButton.setVisibility(VISIBLE);
			declineButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					item.setAnswered(true);
					listener.respondToRequest(item, false);
				}
			});
		}
	}

}

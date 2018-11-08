package org.briarproject.briar.android.conversation;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.UiThread;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.util.BriarAdapter;

import javax.annotation.Nullable;

@NotNullByDefault
class ConversationAdapter
		extends BriarAdapter<ConversationItem, ConversationItemViewHolder> {

	private ConversationListener listener;

	ConversationAdapter(Context ctx,
			ConversationListener conversationListener) {
		super(ctx, ConversationItem.class);
		listener = conversationListener;
	}

	@LayoutRes
	@Override
	public int getItemViewType(int position) {
		ConversationItem item = items.get(position);
		return item.getLayout();
	}

	@Override
	public ConversationItemViewHolder onCreateViewHolder(ViewGroup viewGroup,
			@LayoutRes int type) {
		View v = LayoutInflater.from(viewGroup.getContext()).inflate(
				type, viewGroup, false);
		switch (type) {
			case R.layout.list_item_conversation_msg_in:
				return new ConversationMessageViewHolder(v, true);
			case R.layout.list_item_conversation_msg_out:
				return new ConversationMessageViewHolder(v, false);
			case R.layout.list_item_conversation_notice_in:
				return new ConversationNoticeViewHolder(v, true);
			case R.layout.list_item_conversation_notice_out:
				return new ConversationNoticeViewHolder(v, false);
			case R.layout.list_item_conversation_request:
				return new ConversationRequestViewHolder(v, true);
			default:
				throw new IllegalArgumentException("Unknown ConversationItem");
		}
	}

	@Override
	public void onBindViewHolder(ConversationItemViewHolder ui, int position) {
		ConversationItem item = items.get(position);
		ui.bind(item, listener);
		listener.onItemVisible(item);
	}

	@Override
	public int compare(ConversationItem c1, ConversationItem c2) {
		return Long.compare(c1.getTime(), c2.getTime());
	}

	@Override
	public boolean areItemsTheSame(ConversationItem c1,
			ConversationItem c2) {
		return c1.getId().equals(c2.getId());
	}

	@Override
	public boolean areContentsTheSame(ConversationItem c1,
			ConversationItem c2) {
		return c1.equals(c2);
	}

	@Nullable
	ConversationItem getLastItem() {
		if (items.size() > 0) {
			return items.get(items.size() - 1);
		} else {
			return null;
		}
	}

	SparseArray<ConversationItem> getOutgoingMessages() {
		SparseArray<ConversationItem> messages = new SparseArray<>();

		for (int i = 0; i < items.size(); i++) {
			ConversationItem item = items.get(i);
			if (!item.isIncoming()) {
				messages.put(i, item);
			}
		}
		return messages;
	}

	SparseArray<ConversationMessageItem> getMessageItems() {
		SparseArray<ConversationMessageItem> messages = new SparseArray<>();

		for (int i = 0; i < items.size(); i++) {
			ConversationItem item = items.get(i);
			if (item instanceof ConversationMessageItem) {
				messages.put(i, (ConversationMessageItem) item);
			}
		}
		return messages;
	}

	@UiThread
	@NotNullByDefault
	interface ConversationListener {

		void onItemVisible(ConversationItem item);

		void respondToRequest(ConversationRequestItem item, boolean accept);

		void openRequestedShareable(ConversationRequestItem item);

	}

}

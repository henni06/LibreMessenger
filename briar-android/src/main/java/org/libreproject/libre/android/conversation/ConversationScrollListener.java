package org.libreproject.libre.android.conversation;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.android.view.BriarRecyclerViewScrollListener;

@NotNullByDefault
class ConversationScrollListener extends
		BriarRecyclerViewScrollListener<ConversationAdapter, ConversationItem> {

	private final ConversationViewModel viewModel;

	protected ConversationScrollListener(ConversationAdapter adapter,
			ConversationViewModel viewModel) {
		super(adapter);
		this.viewModel = viewModel;
	}

	@Override
	protected void onItemVisible(ConversationItem item) {
		if (!item.isRead()) {
			viewModel.markMessageRead(item.getGroupId(), item.getId());
			item.markRead();
		}
	}

}

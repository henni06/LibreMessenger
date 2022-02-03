package org.libreproject.libre.android.privategroup.conversation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.R;
import org.libreproject.libre.android.threaded.BaseThreadItemViewHolder;
import org.libreproject.libre.android.threaded.ThreadItemAdapter;
import org.libreproject.libre.android.threaded.ThreadPostViewHolder;

import androidx.annotation.LayoutRes;
import androidx.annotation.UiThread;

@UiThread
@NotNullByDefault
class GroupMessageAdapter extends ThreadItemAdapter<GroupMessageItem> {

	private boolean isCreator = false;

	GroupMessageAdapter(ThreadItemListener<GroupMessageItem> listener) {
		super(listener);
	}

	@LayoutRes
	@Override
	public int getItemViewType(int position) {
		GroupMessageItem item = getItem(position);
		return item.getLayout();
	}

	@Override
	public BaseThreadItemViewHolder<GroupMessageItem> onCreateViewHolder(
			ViewGroup parent, int type) {
		View v = LayoutInflater.from(parent.getContext())
				.inflate(type, parent, false);
		if (type == R.layout.list_item_group_join_notice) {
			return new JoinMessageItemViewHolder(v, isCreator);
		}
		return new ThreadPostViewHolder<>(v);
	}

	void setIsCreator(boolean isCreator) {
		this.isCreator = isCreator;
		notifyDataSetChanged();
	}

}

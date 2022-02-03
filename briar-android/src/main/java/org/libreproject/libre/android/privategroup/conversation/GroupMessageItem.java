package org.libreproject.libre.android.privategroup.conversation;

import org.libreproject.bramble.api.identity.Author;
import org.libreproject.libre.api.identity.AuthorInfo;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.R;
import org.libreproject.libre.android.threaded.ThreadItem;
import org.libreproject.libre.api.privategroup.GroupMessageHeader;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import androidx.annotation.LayoutRes;
import androidx.annotation.UiThread;

@UiThread
@NotThreadSafe
public class GroupMessageItem extends ThreadItem {

	private final GroupId groupId;

	private GroupMessageItem(MessageId messageId, GroupId groupId,
			@Nullable MessageId parentId, String text, long timestamp,
			Author author, AuthorInfo authorInfo, boolean isRead) {
		super(messageId, parentId, text, timestamp, author, authorInfo, isRead);
		this.groupId = groupId;
	}

	public GroupMessageItem(GroupMessageHeader h, String text) {
		this(h.getId(), h.getGroupId(), h.getParentId(), text, h.getTimestamp(),
				h.getAuthor(), h.getAuthorInfo(), h.isRead());
	}

	public GroupId getGroupId() {
		return groupId;
	}

	@LayoutRes
	public int getLayout() {
		return R.layout.list_item_thread;
	}

}

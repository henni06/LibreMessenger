package org.libreproject.libre.android.forum;

import org.libreproject.libre.android.threaded.ThreadItem;
import org.libreproject.libre.api.forum.ForumPostHeader;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
class ForumPostItem extends ThreadItem {

	ForumPostItem(ForumPostHeader h, String text) {
		super(h.getId(), h.getParentId(), text, h.getTimestamp(), h.getAuthor(),
				h.getAuthorInfo(), h.isRead());
	}

}

package org.libreproject.libre.android.blog;

import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.blog.BlogPostHeader;
import org.libreproject.libre.api.identity.AuthorInfo;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import androidx.annotation.NonNull;

@NotThreadSafe
public class BlogPostItem implements Comparable<BlogPostItem> {

	private final BlogPostHeader header;
	@Nullable
	protected String text;
	private final boolean read;

	BlogPostItem(BlogPostHeader header, @Nullable String text) {
		this.header = header;
		this.text = text;
		this.read = header.isRead();
	}

	public MessageId getId() {
		return header.getId();
	}

	public GroupId getGroupId() {
		return header.getGroupId();
	}

	public long getTimestamp() {
		return header.getTimestamp();
	}

	public Author getAuthor() {
		return header.getAuthor();
	}

	AuthorInfo getAuthorInfo() {
		return header.getAuthorInfo();
	}

	@Nullable
	public String getText() {
		return text;
	}

	boolean isRssFeed() {
		return header.isRssFeed();
	}

	public boolean isRead() {
		return read;
	}

	public BlogPostHeader getHeader() {
		return header;
	}

	BlogPostHeader getPostHeader() {
		return getHeader();
	}

	@Override
	public int compareTo(@NonNull BlogPostItem other) {
		if (this == other) return 0;
		return compare(getHeader(), other.getHeader());
	}

	protected static int compare(BlogPostHeader h1, BlogPostHeader h2) {
		// The newest post comes first
		return Long.compare(h2.getTimeReceived(), h1.getTimeReceived());
	}
}

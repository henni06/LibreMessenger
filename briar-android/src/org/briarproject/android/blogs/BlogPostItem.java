package org.briarproject.android.blogs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.briarproject.api.blogs.BlogPostHeader;
import org.briarproject.api.identity.Author;
import org.briarproject.api.identity.Author.Status;
import org.briarproject.api.sync.GroupId;
import org.briarproject.api.sync.MessageId;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class BlogPostItem implements Comparable<BlogPostItem> {

	private final BlogPostHeader header;
	protected String body;
	private boolean read;

	BlogPostItem(BlogPostHeader header, @Nullable String body) {
		this.header = header;
		this.body = body;
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

	Status getAuthorStatus() {
		return header.getAuthorStatus();
	}

	public String getBody() {
		return body;
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
		long aTime = h1.getTimeReceived(), bTime = h2.getTimeReceived();
		if (aTime > bTime) return -1;
		if (aTime < bTime) return 1;
		return 0;
	}
}

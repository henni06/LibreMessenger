package org.libreproject.libre.api.blog;

import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.Group;
import org.libreproject.libre.api.client.BaseGroup;
import org.libreproject.libre.api.sharing.Shareable;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class Blog extends BaseGroup implements Shareable {

	private final Author author;
	private final boolean rssFeed;

	public Blog(Group group, Author author, boolean rssFeed) {
		super(group);
		this.author = author;
		this.rssFeed = rssFeed;
	}

	public Author getAuthor() {
		return author;
	}

	public boolean isRssFeed() {
		return rssFeed;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Blog && super.equals(o);
	}

	/**
	 * Returns the blog's author's name, not the name as shown in the UI.
	 */
	@Override
	public String getName() {
		return author.getName();
	}

}

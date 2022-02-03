package org.libreproject.libre.sharing;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.blog.Blog;
import org.libreproject.libre.api.blog.BlogFactory;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
@NotNullByDefault
class BlogMessageParserImpl extends MessageParserImpl<Blog> {

	private final BlogFactory blogFactory;

	@Inject
	BlogMessageParserImpl(ClientHelper clientHelper, BlogFactory blogFactory) {
		super(clientHelper);
		this.blogFactory = blogFactory;
	}

	@Override
	public Blog createShareable(BdfList descriptor) throws FormatException {
		// Author, RSS
		BdfList authorList = descriptor.getList(0);
		boolean rssFeed = descriptor.getBoolean(1);

		Author author = clientHelper.parseAndValidateAuthor(authorList);
		if (rssFeed) return blogFactory.createFeedBlog(author);
		else return blogFactory.createBlog(author);
	}

}

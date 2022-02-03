package org.libreproject.libre.sharing;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.libre.api.blog.BlogFactory;

import javax.annotation.concurrent.Immutable;

import static org.libreproject.bramble.util.ValidationUtils.checkSize;

@Immutable
@NotNullByDefault
class BlogSharingValidator extends SharingValidator {

	private final BlogFactory blogFactory;

	BlogSharingValidator(MessageEncoder messageEncoder,
			ClientHelper clientHelper, MetadataEncoder metadataEncoder,
			Clock clock, BlogFactory blogFactory) {
		super(messageEncoder, clientHelper, metadataEncoder, clock);
		this.blogFactory = blogFactory;
	}

	@Override
	protected GroupId validateDescriptor(BdfList descriptor)
			throws FormatException {
		// Author, RSS
		checkSize(descriptor, 2);
		BdfList authorList = descriptor.getList(0);
		boolean rssFeed = descriptor.getBoolean(1);
		Author author = clientHelper.parseAndValidateAuthor(authorList);
		if (rssFeed) return blogFactory.createFeedBlog(author).getId();
		else return blogFactory.createBlog(author).getId();
	}

}

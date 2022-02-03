package org.libreproject.libre.sharing;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.forum.Forum;
import org.libreproject.libre.api.forum.ForumFactory;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
@NotNullByDefault
class ForumMessageParserImpl extends MessageParserImpl<Forum> {

	private final ForumFactory forumFactory;

	@Inject
	ForumMessageParserImpl(ClientHelper clientHelper,
			ForumFactory forumFactory) {
		super(clientHelper);
		this.forumFactory = forumFactory;
	}

	@Override
	public Forum createShareable(BdfList descriptor) throws FormatException {
		// Name, salt
		String name = descriptor.getString(0);
		byte[] salt = descriptor.getRaw(1);
		return forumFactory.createForum(name, salt);
	}

}

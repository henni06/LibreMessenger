package org.libreproject.libre.sharing;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.libre.api.forum.Forum;
import org.libreproject.libre.api.forum.ForumFactory;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.libreproject.bramble.util.ValidationUtils.checkLength;
import static org.libreproject.bramble.util.ValidationUtils.checkSize;
import static org.libreproject.libre.api.forum.ForumConstants.FORUM_SALT_LENGTH;
import static org.libreproject.libre.api.forum.ForumConstants.MAX_FORUM_NAME_LENGTH;

@Immutable
@NotNullByDefault
class ForumSharingValidator extends SharingValidator {

	private final ForumFactory forumFactory;

	@Inject
	ForumSharingValidator(MessageEncoder messageEncoder,
			ClientHelper clientHelper, MetadataEncoder metadataEncoder,
			Clock clock, ForumFactory forumFactory) {
		super(messageEncoder, clientHelper, metadataEncoder, clock);
		this.forumFactory = forumFactory;
	}

	@Override
	protected GroupId validateDescriptor(BdfList descriptor)
			throws FormatException {
		// Name, salt
		checkSize(descriptor, 2);
		String name = descriptor.getString(0);
		checkLength(name, 1, MAX_FORUM_NAME_LENGTH);
		byte[] salt = descriptor.getRaw(1);
		checkLength(salt, FORUM_SALT_LENGTH);
		Forum forum = forumFactory.createForum(name, salt);
		return forum.getId();
	}

}

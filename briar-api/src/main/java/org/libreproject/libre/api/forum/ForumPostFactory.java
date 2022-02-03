package org.libreproject.libre.api.forum;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.crypto.CryptoExecutor;
import org.libreproject.bramble.api.identity.LocalAuthor;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;

import java.security.GeneralSecurityException;

import javax.annotation.Nullable;

import static org.libreproject.libre.api.forum.ForumManager.CLIENT_ID;

@NotNullByDefault
public interface ForumPostFactory {

	String SIGNING_LABEL_POST = CLIENT_ID.getString() + "/POST";

	@CryptoExecutor
	ForumPost createPost(GroupId groupId, long timestamp,
			@Nullable MessageId parent, LocalAuthor author, String text)
			throws FormatException, GeneralSecurityException;

}

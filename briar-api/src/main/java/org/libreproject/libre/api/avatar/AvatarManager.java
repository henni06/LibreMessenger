package org.libreproject.libre.api.avatar;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.ClientId;
import org.libreproject.libre.api.attachment.AttachmentHeader;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

@NotNullByDefault
public interface AvatarManager {

	/**
	 * The unique ID of the avatar client.
	 */
	ClientId CLIENT_ID = new ClientId("org.briarproject.briar.avatar");

	/**
	 * The current major version of the avatar client.
	 */
	int MAJOR_VERSION = 0;

	/**
	 * The current minor version of the avatar client.
	 */
	int MINOR_VERSION = 0;

	/**
	 * Store a new profile image represented by the given InputStream
	 * and share it with all contacts.
	 */
	AttachmentHeader addAvatar(String contentType, InputStream in)
			throws DbException, IOException;

	/**
	 * Returns the current known profile image header for the given contact
	 * or null if none is known.
	 */
	@Nullable
	AttachmentHeader getAvatarHeader(Transaction txn, Contact c)
			throws DbException;

	/**
	 * Returns our current profile image header or null if none has been added.
	 */
	@Nullable
	AttachmentHeader getMyAvatarHeader(Transaction txn) throws DbException;
}

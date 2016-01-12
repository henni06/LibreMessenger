package org.briarproject.sync;

import org.briarproject.api.FormatException;
import org.briarproject.api.crypto.CryptoComponent;
import org.briarproject.api.crypto.MessageDigest;
import org.briarproject.api.data.BdfReader;
import org.briarproject.api.data.ObjectReader;
import org.briarproject.api.sync.Group;
import org.briarproject.api.sync.GroupId;

import java.io.IOException;

import static org.briarproject.api.sync.MessagingConstants.GROUP_SALT_LENGTH;
import static org.briarproject.api.sync.MessagingConstants.MAX_GROUP_NAME_LENGTH;

class GroupReader implements ObjectReader<Group> {

	private final MessageDigest messageDigest;

	GroupReader(CryptoComponent crypto) {
		messageDigest = crypto.getMessageDigest();
	}

	public Group readObject(BdfReader r) throws IOException {
		DigestingConsumer digesting = new DigestingConsumer(messageDigest);
		// Read and digest the data
		r.addConsumer(digesting);
		r.readListStart();
		String name = r.readString(MAX_GROUP_NAME_LENGTH);
		if (name.length() == 0) throw new FormatException();
		byte[] salt = r.readRaw(GROUP_SALT_LENGTH);
		if (salt.length != GROUP_SALT_LENGTH) throw new FormatException();
		r.readListEnd();
		r.removeConsumer(digesting);
		// Build and return the group
		GroupId id = new GroupId(messageDigest.digest());
		return new Group(id, name, salt);
	}
}

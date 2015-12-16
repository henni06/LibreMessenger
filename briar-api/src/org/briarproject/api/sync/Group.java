package org.briarproject.api.sync;

import java.io.UnsupportedEncodingException;

/** A group to which users may subscribe. */
public class Group {

	private final GroupId id;
	private final String name;
	private final byte[] salt;

	public Group(GroupId id, String name, byte[] salt) {
		int length;
		try {
			length = name.getBytes("UTF-8").length;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		if (length == 0 || length > MessagingConstants.MAX_GROUP_NAME_LENGTH)
			throw new IllegalArgumentException();
		if (salt.length != MessagingConstants.GROUP_SALT_LENGTH)
			throw new IllegalArgumentException();
		this.id = id;
		this.name = name;
		this.salt = salt;
	}

	/** Returns the group's unique identifier. */
	public GroupId getId() {
		return id;
	}

	/** Returns the group's name. */
	public String getName() {
		return name;
	}

	/**
	 * Returns the salt used to distinguish the group from other groups with
	 * the same name.
	 */
	public byte[] getSalt() {
		return salt;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Group && id.equals(((Group) o).id);
	}
}

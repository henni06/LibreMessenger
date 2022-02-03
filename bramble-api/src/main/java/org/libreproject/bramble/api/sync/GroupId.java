package org.libreproject.bramble.api.sync;

import org.libreproject.bramble.api.UniqueId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Type-safe wrapper for a byte array that uniquely identifies a {@link Group}.
 */
@ThreadSafe
@NotNullByDefault
public class GroupId extends UniqueId {

	/**
	 * Label for hashing groups to calculate their identifiers.
	 */
	public static final String LABEL = "org.briarproject.bramble/GROUP_ID";

	public GroupId(byte[] id) {
		super(id);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof GroupId && super.equals(o);
	}
}

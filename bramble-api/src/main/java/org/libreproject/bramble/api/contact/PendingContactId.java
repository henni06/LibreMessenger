package org.libreproject.bramble.api.contact;

import org.libreproject.bramble.api.UniqueId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Type-safe wrapper for a byte array that uniquely identifies a
 * {@link PendingContact}.
 */
@ThreadSafe
@NotNullByDefault
public class PendingContactId extends UniqueId {

	public PendingContactId(byte[] id) {
		super(id);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		return o instanceof PendingContactId && super.equals(o);
	}
}

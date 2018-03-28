package org.briarproject.bramble.api.transport;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/**
 * Type-safe wrapper for an integer that uniquely identifies a set of transport
 * keys within the scope of the local device.
 * <p/>
 * Key sets created on a given device must have increasing identifiers.
 */
@Immutable
@NotNullByDefault
public class KeySetId {

	private final int id;

	public KeySetId(int id) {
		this.id = id;
	}

	public int getInt() {
		return id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof KeySetId && id == ((KeySetId) o).id;
	}
}

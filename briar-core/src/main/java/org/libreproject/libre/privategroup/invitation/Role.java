package org.libreproject.libre.privategroup.invitation;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
enum Role {

	CREATOR(0), INVITEE(1), PEER(2);

	private final int value;

	Role(int value) {
		this.value = value;
	}

	int getValue() {
		return value;
	}

	static Role fromValue(int value) throws FormatException {
		for (Role r : values()) if (r.value == value) return r;
		throw new FormatException();
	}
}

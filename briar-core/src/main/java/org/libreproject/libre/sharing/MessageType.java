package org.libreproject.libre.sharing;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
enum MessageType {

	INVITE(0), ACCEPT(1), DECLINE(2), LEAVE(3), ABORT(4);

	private final int value;

	MessageType(int value) {
		this.value = value;
	}

	int getValue() {
		return value;
	}

	static MessageType fromValue(int value) throws FormatException {
		for (MessageType m : values()) if (m.value == value) return m;
		throw new FormatException();
	}

}

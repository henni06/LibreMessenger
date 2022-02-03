package org.libreproject.libre.api.privategroup;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public enum MessageType {

	JOIN(0),
	POST(1),
	LOCATION(2),
	CALENDAR(3),
	TASK(4);
	private final int value;

	MessageType(int value) {
		this.value = value;
	}

	public static MessageType valueOf(int value) {
		for (MessageType m : values()) if (m.value == value) return m;
		throw new IllegalArgumentException();
	}

	public int getInt() {
		return value;
	}
}

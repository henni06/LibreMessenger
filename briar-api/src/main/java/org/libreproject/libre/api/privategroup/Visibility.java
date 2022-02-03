package org.libreproject.libre.api.privategroup;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public enum Visibility {

	INVISIBLE(0),
	VISIBLE(1),
	REVEALED_BY_US(2),
	REVEALED_BY_CONTACT(3);

	private final int value;

	Visibility(int value) {
		this.value = value;
	}

	public static Visibility valueOf(int value) throws FormatException {
		for (Visibility v : values()) if (v.value == value) return v;
		throw new FormatException();
	}

	public int getInt() {
		return value;
	}

}

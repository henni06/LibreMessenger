package org.libreproject.libre.api.forum;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.Group;
import org.libreproject.libre.api.client.NamedGroup;
import org.libreproject.libre.api.sharing.Shareable;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class Forum extends NamedGroup implements Shareable {

	public Forum(Group group, String name, byte[] salt) {
		super(group, name, salt);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Forum && super.equals(o);
	}

}

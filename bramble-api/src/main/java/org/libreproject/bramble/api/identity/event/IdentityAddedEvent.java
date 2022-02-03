package org.libreproject.bramble.api.identity.event;

import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.identity.AuthorId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when an identity is added.
 */
@Immutable
@NotNullByDefault
public class IdentityAddedEvent extends Event {

	private final AuthorId authorId;

	public IdentityAddedEvent(AuthorId authorId) {
		this.authorId = authorId;
	}

	public AuthorId getAuthorId() {
		return authorId;
	}
}

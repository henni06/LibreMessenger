package org.libreproject.bramble.api.sync.event;

import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.Group;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a group is added to the database.
 */
@Immutable
@NotNullByDefault
public class GroupAddedEvent extends Event {

	private final Group group;

	public GroupAddedEvent(Group group) {
		this.group = group;
	}

	public Group getGroup() {
		return group;
	}
}

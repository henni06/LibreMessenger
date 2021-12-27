package org.briarproject.briar.api.privategroup.event;

import org.briarproject.bramble.api.event.Event;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.briar.api.privategroup.GroupMessageHeader;
import org.briarproject.briar.api.privategroup.location.LocationMessageHeader;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class LocationMessageAddEvent extends Event {
	private final GroupId groupId;
	private final LocationMessageHeader header;
	private final String text;
	private final boolean local;

	public LocationMessageAddEvent(GroupId groupId, LocationMessageHeader header,
			String text, boolean local) {
		this.groupId = groupId;
		this.header = header;
		this.text = text;
		this.local = local;
	}

	public GroupId getGroupId() {
		return groupId;
	}

	public LocationMessageHeader getHeader() {
		return header;
	}

	public String getText() {
		return text;
	}

	public boolean isLocal() {
		return local;
	}
}

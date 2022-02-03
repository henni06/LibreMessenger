package org.libreproject.libre.api.privategroup.event;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.identity.AuthorId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.api.privategroup.Visibility;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class ContactRelationshipRevealedEvent extends Event {

	private final GroupId groupId;
	private final AuthorId memberId;
	private final ContactId contactId;
	private final Visibility visibility;

	public ContactRelationshipRevealedEvent(GroupId groupId, AuthorId memberId,
			ContactId contactId, Visibility visibility) {
		this.groupId = groupId;
		this.memberId = memberId;
		this.contactId = contactId;
		this.visibility = visibility;
	}

	public GroupId getGroupId() {
		return groupId;
	}

	public AuthorId getMemberId() {
		return memberId;
	}

	public ContactId getContactId() {
		return contactId;
	}

	public Visibility getVisibility() {
		return visibility;
	}

}

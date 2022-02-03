package org.libreproject.libre.api.privategroup.invitation;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.privategroup.PrivateGroup;
import org.libreproject.libre.api.sharing.InvitationItem;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class GroupInvitationItem extends InvitationItem<PrivateGroup> {

	private final Contact creator;

	public GroupInvitationItem(PrivateGroup privateGroup, Contact creator) {
		super(privateGroup, false);
		this.creator = creator;
	}

	public Contact getCreator() {
		return creator;
	}

}

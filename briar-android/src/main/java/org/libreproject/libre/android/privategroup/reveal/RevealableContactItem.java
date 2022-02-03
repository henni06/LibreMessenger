package org.libreproject.libre.android.privategroup.reveal;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.android.contactselection.SelectableContactItem;
import org.libreproject.libre.api.identity.AuthorInfo;
import org.libreproject.libre.api.privategroup.Visibility;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
@NotNullByDefault
class RevealableContactItem extends SelectableContactItem {

	private final Visibility visibility;

	RevealableContactItem(Contact contact, AuthorInfo authorInfo,
			boolean selected, boolean disabled, Visibility visibility) {
		super(contact, authorInfo, selected, disabled);
		this.visibility = visibility;
	}

	Visibility getVisibility() {
		return visibility;
	}

}

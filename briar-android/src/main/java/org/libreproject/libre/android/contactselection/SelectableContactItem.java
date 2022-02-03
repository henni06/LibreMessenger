package org.libreproject.libre.android.contactselection;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.android.contact.ContactItem;
import org.libreproject.libre.api.identity.AuthorInfo;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
@NotNullByDefault
public class SelectableContactItem extends ContactItem {

	private boolean selected;
	private final boolean disabled;

	public SelectableContactItem(Contact contact, AuthorInfo authorInfo,
			boolean selected, boolean disabled) {
		super(contact, authorInfo);
		this.selected = selected;
		this.disabled = disabled;
	}

	boolean isSelected() {
		return selected;
	}

	void toggleSelected() {
		selected = !selected;
	}

	public boolean isDisabled() {
		return disabled;
	}

}

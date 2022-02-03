package org.libreproject.libre.android.contact;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.identity.AuthorInfo;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class ContactItem {

	private final Contact contact;
	private final AuthorInfo authorInfo;
	private final boolean connected;

	public ContactItem(Contact contact, AuthorInfo authorInfo) {
		this(contact, authorInfo, false);
	}

	public ContactItem(Contact contact, AuthorInfo authorInfo,
			boolean connected) {
		this.contact = contact;
		this.authorInfo = authorInfo;
		this.connected = connected;
	}

	public Contact getContact() {
		return contact;
	}

	public AuthorInfo getAuthorInfo() {
		return authorInfo;
	}

	boolean isConnected() {
		return connected;
	}

}

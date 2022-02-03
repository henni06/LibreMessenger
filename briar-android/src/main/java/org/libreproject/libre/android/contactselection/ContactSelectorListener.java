package org.libreproject.libre.android.contactselection;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.util.Collection;

import androidx.annotation.UiThread;

@NotNullByDefault
public interface ContactSelectorListener {

	@UiThread
	void contactsSelected(Collection<ContactId> contacts);

}

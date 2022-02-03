package org.libreproject.libre.android.contactselection;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.android.controller.DbController;
import org.libreproject.libre.android.controller.handler.ResultExceptionHandler;

import java.util.Collection;

@NotNullByDefault
public interface ContactSelectorController<I extends SelectableContactItem>
		extends DbController {

	void loadContacts(GroupId g, Collection<ContactId> selection,
			ResultExceptionHandler<Collection<I>, DbException> handler);

}

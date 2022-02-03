package org.libreproject.libre.android.sharing;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.android.contactselection.ContactSelectorController;
import org.libreproject.libre.android.contactselection.SelectableContactItem;
import org.libreproject.libre.android.controller.handler.ExceptionHandler;

import java.util.Collection;

import javax.annotation.Nullable;

public interface ShareForumController
		extends ContactSelectorController<SelectableContactItem> {

	void share(GroupId g, Collection<ContactId> contacts, @Nullable String text,
			ExceptionHandler<DbException> handler);

}

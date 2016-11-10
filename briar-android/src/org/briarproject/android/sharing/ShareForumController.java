package org.briarproject.android.sharing;

import org.briarproject.android.contactselection.ContactSelectorController;
import org.briarproject.android.contactselection.SelectableContactItem;
import org.briarproject.android.controller.handler.ResultExceptionHandler;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.db.DbException;
import org.briarproject.api.sync.GroupId;

import java.util.Collection;

public interface ShareForumController
		extends ContactSelectorController<SelectableContactItem> {

	void share(GroupId g, Collection<ContactId> contacts, String msg,
			ResultExceptionHandler<Void, DbException> handler);

}

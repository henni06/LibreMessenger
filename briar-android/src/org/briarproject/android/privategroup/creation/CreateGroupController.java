package org.briarproject.android.privategroup.creation;

import org.briarproject.android.contactselection.ContactSelectorController;
import org.briarproject.android.contactselection.SelectableContactItem;
import org.briarproject.android.controller.handler.ResultExceptionHandler;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.db.DbException;
import org.briarproject.api.nullsafety.NotNullByDefault;
import org.briarproject.api.sync.GroupId;

import java.util.Collection;

@NotNullByDefault
public interface CreateGroupController
		extends ContactSelectorController<SelectableContactItem> {

	void createGroup(String name,
			ResultExceptionHandler<GroupId, DbException> result);

	void sendInvitation(GroupId g, Collection<ContactId> contacts,
			String message, ResultExceptionHandler<Void, DbException> result);

}

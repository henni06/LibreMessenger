package org.libreproject.libre.android.privategroup.creation;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.android.contactselection.ContactSelectorController;
import org.libreproject.libre.android.contactselection.SelectableContactItem;
import org.libreproject.libre.android.controller.handler.ResultExceptionHandler;

import java.util.Collection;

import androidx.annotation.Nullable;

@NotNullByDefault
public interface CreateGroupController
		extends ContactSelectorController<SelectableContactItem> {

	void createGroup(String name,
			ResultExceptionHandler<GroupId, DbException> result);

	void sendInvitation(GroupId g, Collection<ContactId> contacts,
			@Nullable String text,
			ResultExceptionHandler<Void, DbException> result);

}

package org.libreproject.libre.android.privategroup.reveal;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.android.contactselection.ContactSelectorController;
import org.libreproject.libre.android.controller.handler.ExceptionHandler;
import org.libreproject.libre.android.controller.handler.ResultExceptionHandler;

import java.util.Collection;

@NotNullByDefault
public interface RevealContactsController
		extends ContactSelectorController<RevealableContactItem> {

	void isOnboardingNeeded(
			ResultExceptionHandler<Boolean, DbException> handler);

	void onboardingShown(ExceptionHandler<DbException> handler);

	void reveal(GroupId g, Collection<ContactId> contacts,
			ExceptionHandler<DbException> handler);

}

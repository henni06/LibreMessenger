package org.libreproject.libre.android.sharing;

import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.android.controller.ActivityLifecycleController;
import org.libreproject.libre.android.controller.handler.ExceptionHandler;
import org.libreproject.libre.android.controller.handler.ResultExceptionHandler;
import org.libreproject.libre.api.sharing.InvitationItem;

import java.util.Collection;

@NotNullByDefault
public interface InvitationController<I extends InvitationItem>
		extends ActivityLifecycleController {

	void loadInvitations(boolean clear,
			ResultExceptionHandler<Collection<I>, DbException> handler);

	void respondToInvitation(I item, boolean accept,
			ExceptionHandler<DbException> handler);

	interface InvitationListener {

		void loadInvitations(boolean clear);

	}

}

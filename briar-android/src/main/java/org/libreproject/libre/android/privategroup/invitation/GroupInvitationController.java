package org.libreproject.libre.android.privategroup.invitation;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.android.sharing.InvitationController;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationItem;

@NotNullByDefault
interface GroupInvitationController
		extends InvitationController<GroupInvitationItem> {
}

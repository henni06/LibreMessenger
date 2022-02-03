package org.libreproject.libre.android.privategroup.invitation;

import android.content.Context;

import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.libre.R;
import org.libreproject.libre.android.activity.ActivityComponent;
import org.libreproject.libre.android.sharing.InvitationActivity;
import org.libreproject.libre.android.sharing.InvitationAdapter;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationItem;

import javax.inject.Inject;

import static org.libreproject.libre.android.sharing.InvitationAdapter.InvitationClickListener;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class GroupInvitationActivity
		extends InvitationActivity<GroupInvitationItem> {

	@Inject
	protected GroupInvitationController controller;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	protected GroupInvitationController getController() {
		return controller;
	}

	@Override
	protected InvitationAdapter<GroupInvitationItem, ?> getAdapter(Context ctx,
			InvitationClickListener<GroupInvitationItem> listener) {
		return new GroupInvitationAdapter(ctx, listener);
	}

	@Override
	protected int getAcceptRes() {
		return R.string.groups_invitations_joined;
	}

	@Override
	protected int getDeclineRes() {
		return R.string.groups_invitations_declined;
	}

}

package org.libreproject.libre.android.privategroup.invitation;

import android.view.View;

import org.libreproject.libre.R;
import org.libreproject.libre.android.sharing.InvitationAdapter.InvitationClickListener;
import org.libreproject.libre.android.sharing.InvitationViewHolder;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationItem;

import javax.annotation.Nullable;

import static org.libreproject.libre.android.util.UiUtils.getContactDisplayName;

class GroupInvitationViewHolder
		extends InvitationViewHolder<GroupInvitationItem> {

	GroupInvitationViewHolder(View v) {
		super(v);
	}

	@Override
	public void onBind(@Nullable GroupInvitationItem item,
			InvitationClickListener<GroupInvitationItem> listener) {
		super.onBind(item, listener);
		if (item == null) return;

		sharedBy.setText(
				sharedBy.getContext().getString(R.string.groups_created_by,
						getContactDisplayName(item.getCreator())));
	}

}
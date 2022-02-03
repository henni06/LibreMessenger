package org.libreproject.libre.android.sharing;

import android.view.View;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.util.StringUtils;
import org.libreproject.libre.R;
import org.libreproject.libre.android.sharing.InvitationAdapter.InvitationClickListener;
import org.libreproject.libre.api.sharing.SharingInvitationItem;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

import static org.libreproject.libre.android.util.UiUtils.getContactDisplayName;

class SharingInvitationViewHolder
		extends InvitationViewHolder<SharingInvitationItem> {

	SharingInvitationViewHolder(View v) {
		super(v);
	}

	@Override
	public void onBind(@Nullable SharingInvitationItem item,
			InvitationClickListener<SharingInvitationItem> listener) {
		super.onBind(item, listener);
		if (item == null) return;

		Collection<String> names = new ArrayList<>();
		for (Contact c : item.getNewSharers())
			names.add(getContactDisplayName(c));
		sharedBy.setText(
				sharedBy.getContext().getString(R.string.shared_by_format,
						StringUtils.join(names, ", ")));
	}

}

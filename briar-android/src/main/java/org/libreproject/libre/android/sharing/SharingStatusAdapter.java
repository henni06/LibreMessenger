package org.libreproject.libre.android.sharing;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.libreproject.libre.R;
import org.libreproject.libre.android.contact.BaseContactListAdapter;
import org.libreproject.libre.android.contact.ContactItem;
import org.libreproject.libre.android.contact.ContactItemViewHolder;

class SharingStatusAdapter extends
		BaseContactListAdapter<ContactItem, ContactItemViewHolder<ContactItem>> {

	SharingStatusAdapter(Context context) {
		super(context, ContactItem.class, null);
	}

	@Override
	public ContactItemViewHolder<ContactItem> onCreateViewHolder(
			ViewGroup viewGroup, int i) {
		View v = LayoutInflater.from(viewGroup.getContext()).inflate(
				R.layout.list_item_contact_small, viewGroup, false);
		return new ContactItemViewHolder<>(v);
	}

}

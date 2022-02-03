package org.libreproject.libre.android.contactselection;

import android.content.Context;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.android.contact.BaseContactListAdapter;
import org.libreproject.libre.android.contact.ContactItemViewHolder;
import org.libreproject.libre.android.contact.OnContactClickListener;

import java.util.ArrayList;
import java.util.Collection;

@NotNullByDefault
public abstract class BaseContactSelectorAdapter<I extends SelectableContactItem, H extends ContactItemViewHolder<I>>
		extends BaseContactListAdapter<I, H> {

	public BaseContactSelectorAdapter(Context context, Class<I> c,
			OnContactClickListener<I> listener) {
		super(context, c, listener);
	}

	public Collection<ContactId> getSelectedContactIds() {
		Collection<ContactId> selected = new ArrayList<>();

		for (int i = 0; i < items.size(); i++) {
			SelectableContactItem item = items.get(i);
			if (item.isSelected()) selected.add(item.getContact().getId());
		}
		return selected;
	}

}

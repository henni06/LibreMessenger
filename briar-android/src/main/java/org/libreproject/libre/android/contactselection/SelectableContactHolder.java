package org.libreproject.libre.android.contactselection;

import android.view.View;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.android.contact.OnContactClickListener;

import javax.annotation.Nullable;

import androidx.annotation.UiThread;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

@UiThread
@NotNullByDefault
class SelectableContactHolder
		extends BaseSelectableContactHolder<SelectableContactItem> {

	SelectableContactHolder(View v) {
		super(v);
	}

	@Override
	protected void bind(SelectableContactItem item, @Nullable
			OnContactClickListener<SelectableContactItem> listener) {
		super.bind(item, listener);

		if (item.isDisabled()) {
			info.setVisibility(VISIBLE);
		} else {
			info.setVisibility(GONE);
		}
	}

}

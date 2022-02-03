package org.libreproject.libre.android.privategroup.reveal;

import android.view.View;
import android.widget.ImageView;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.R;
import org.libreproject.libre.android.contact.OnContactClickListener;
import org.libreproject.libre.android.contactselection.BaseSelectableContactHolder;

import javax.annotation.Nullable;

import androidx.annotation.UiThread;

import static org.libreproject.libre.android.privategroup.VisibilityHelper.getVisibilityIcon;
import static org.libreproject.libre.android.privategroup.VisibilityHelper.getVisibilityString;
import static org.libreproject.libre.android.util.UiUtils.GREY_OUT;
import static org.libreproject.libre.android.util.UiUtils.getContactDisplayName;

@UiThread
@NotNullByDefault
class RevealableContactViewHolder
		extends BaseSelectableContactHolder<RevealableContactItem> {

	private final ImageView icon;

	RevealableContactViewHolder(View v) {
		super(v);

		icon = v.findViewById(R.id.visibilityView);
	}

	@Override
	protected void bind(RevealableContactItem item, @Nullable
			OnContactClickListener<RevealableContactItem> listener) {
		super.bind(item, listener);

		icon.setImageResource(getVisibilityIcon(item.getVisibility()));
		info.setText(
				getVisibilityString(info.getContext(), item.getVisibility(),
						getContactDisplayName(item.getContact())));
	}

	@Override
	protected void grayOutItem(boolean gray) {
		super.grayOutItem(gray);
		float alpha = gray ? GREY_OUT : 1f;
		icon.setAlpha(alpha);
	}

}

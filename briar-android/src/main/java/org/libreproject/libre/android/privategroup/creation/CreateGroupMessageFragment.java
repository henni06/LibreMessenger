package org.libreproject.libre.android.privategroup.creation;

import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.libre.R;
import org.libreproject.libre.android.sharing.BaseMessageFragment;

import androidx.annotation.StringRes;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class CreateGroupMessageFragment extends BaseMessageFragment {

	private final static String TAG =
			CreateGroupMessageFragment.class.getName();

	@Override
	@StringRes
	protected int getButtonText() {
		return R.string.groups_create_group_invitation_button;
	}

	@Override
	@StringRes
	protected int getHintText() {
		return R.string.forum_share_message;
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

}

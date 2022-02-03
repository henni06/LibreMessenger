package org.libreproject.libre.android.sharing;

import android.os.Bundle;

import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.android.activity.ActivityComponent;
import org.libreproject.libre.android.contactselection.ContactSelectorController;
import org.libreproject.libre.android.contactselection.ContactSelectorFragment;
import org.libreproject.libre.android.contactselection.SelectableContactItem;

import javax.inject.Inject;

import static org.libreproject.libre.android.activity.BriarActivity.GROUP_ID;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class ShareForumFragment extends ContactSelectorFragment {

	public static final String TAG = ShareForumFragment.class.getName();

	@Inject
	ShareForumController controller;

	public static ShareForumFragment newInstance(GroupId groupId) {
		Bundle args = new Bundle();
		args.putByteArray(GROUP_ID, groupId.getBytes());
		ShareForumFragment fragment = new ShareForumFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void injectFragment(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	protected ContactSelectorController<SelectableContactItem> getController() {
		return controller;
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

}

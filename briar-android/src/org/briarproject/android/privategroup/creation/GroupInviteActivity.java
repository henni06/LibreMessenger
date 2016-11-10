package org.briarproject.android.privategroup.creation;

import android.content.Intent;
import android.os.Bundle;

import org.briarproject.R;
import org.briarproject.android.ActivityComponent;
import org.briarproject.android.contactselection.ContactSelectorFragment;
import org.briarproject.android.sharing.BaseMessageFragment.MessageFragmentListener;
import org.briarproject.api.sync.GroupId;

public class GroupInviteActivity extends BaseGroupInviteActivity
		implements MessageFragmentListener {

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		Intent i = getIntent();
		byte[] g = i.getByteArrayExtra(GROUP_ID);
		if (g == null) throw new IllegalStateException("No GroupId in intent.");
		groupId = new GroupId(g);

		if (bundle == null) {
			ContactSelectorFragment fragment =
					ContactSelectorFragment.newInstance(groupId);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.fragmentContainer, fragment)
					.commit();
		}
	}

}

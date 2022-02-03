package org.libreproject.libre.android.blog;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.R;
import org.libreproject.libre.android.activity.ActivityComponent;
import org.libreproject.libre.android.activity.BriarActivity;
import org.libreproject.libre.android.fragment.BaseFragment.BaseFragmentListener;

import static org.libreproject.libre.android.blog.BlogPostFragment.POST_ID;

public class ReblogActivity extends BriarActivity implements
		BaseFragmentListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		byte[] groupId = intent.getByteArrayExtra(GROUP_ID);
		if (groupId == null)
			throw new IllegalArgumentException("No group ID in intent");
		byte[] postId = intent.getByteArrayExtra(POST_ID);
		if (postId == null)
			throw new IllegalArgumentException("No post message ID in intent");

		setContentView(R.layout.activity_fragment_container);

		if (savedInstanceState == null) {
			ReblogFragment f = ReblogFragment
					.newInstance(new GroupId(groupId), new MessageId(postId));
			showInitialFragment(f);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

}

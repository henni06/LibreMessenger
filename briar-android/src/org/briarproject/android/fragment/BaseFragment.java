package org.briarproject.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

import org.briarproject.android.ActivityComponent;
import org.briarproject.android.DestroyableContext;

public abstract class BaseFragment extends Fragment
		implements DestroyableContext {

	protected BaseFragmentListener listener;

	public abstract String getUniqueTag();

	public abstract void injectFragment(ActivityComponent component);

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		listener = (BaseFragmentListener) context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// allow for "up" button to act as back button
		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		injectFragment(listener.getActivityComponent());
		listener.onFragmentCreated(getUniqueTag());
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				listener.onBackPressed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@UiThread
	protected void finish() {
		if (!isDetached())
			getActivity().supportFinishAfterTransition();
	}

	public interface BaseFragmentListener {

		@Deprecated
		void runOnDbThread(Runnable runnable);

		@UiThread
		void onBackPressed();

		@UiThread
		ActivityComponent getActivityComponent();

		@UiThread
		void onFragmentCreated(String tag);
	}

	@CallSuper
	@Override
	public void runOnUiThreadUnlessDestroyed(final Runnable r) {
		final Activity activity = getActivity();
		if (activity != null) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// Note that we don't have to check if the activity has
					// been destroyed as the Fragment has not been detached yet
					if (!isDetached() && !activity.isFinishing()) {
						r.run();
					}
				}
			});
		}
	}
}

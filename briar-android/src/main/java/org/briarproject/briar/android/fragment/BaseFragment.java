package org.briarproject.briar.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;

import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.briar.android.DestroyableContext;
import org.briarproject.briar.android.activity.ActivityComponent;

import javax.annotation.Nullable;

import androidx.annotation.CallSuper;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public abstract class BaseFragment extends Fragment
		implements DestroyableContext {

	protected BaseFragmentListener listener;

	public abstract String getUniqueTag();

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		listener = (BaseFragmentListener) context;
		injectFragment(listener.getActivityComponent());
	}

	public void injectFragment(ActivityComponent component) {
		// fragments that need to inject, can override this method
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// allow for "up" button to act as back button
		setHasOptionsMenu(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
		FragmentActivity activity = getActivity();
		if (activity != null) activity.supportFinishAfterTransition();
	}

	public interface BaseFragmentListener {
		@Deprecated
		void runOnDbThread(Runnable runnable);

		@UiThread
		void onBackPressed();

		@UiThread
		ActivityComponent getActivityComponent();

		@UiThread
		void showNextFragment(BaseFragment f);

		@UiThread
		void handleException(Exception e);
	}

	@CallSuper
	@Override
	public void runOnUiThreadUnlessDestroyed(Runnable r) {
		Activity activity = getActivity();
		if (activity != null) {
			activity.runOnUiThread(() -> {
				// Note that we don't have to check if the activity has
				// been destroyed as the Fragment has not been detached yet
				if (isAdded() && !activity.isFinishing()) {
					r.run();
				}
			});
		}
	}

	protected void showNextFragment(BaseFragment f) {
		listener.showNextFragment(f);
	}

	@UiThread
	protected void handleException(Exception e) {
		listener.handleException(e);
	}

}

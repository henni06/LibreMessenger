package org.libreproject.libre.android.account;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;

import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.libre.R;
import org.libreproject.libre.android.activity.ActivityComponent;
import org.libreproject.libre.android.activity.BaseActivity;
import org.libreproject.libre.android.fragment.BaseFragment.BaseFragmentListener;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.lifecycle.ViewModelProvider;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_TASK_ON_HOME;
import static org.libreproject.libre.android.LibreApplication.ENTRY_ACTIVITY;
import static org.libreproject.libre.android.account.SetupViewModel.State.AUTHOR_NAME;
import static org.libreproject.libre.android.account.SetupViewModel.State.CREATED;
import static org.libreproject.libre.android.account.SetupViewModel.State.DOZE;
import static org.libreproject.libre.android.account.SetupViewModel.State.FAILED;
import static org.libreproject.libre.android.account.SetupViewModel.State.SET_PASSWORD;
import static org.libreproject.libre.android.util.UiUtils.setInputStateAlwaysVisible;
import static org.libreproject.libre.android.util.UiUtils.setInputStateHidden;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class SetupActivity extends BaseActivity
		implements BaseFragmentListener {

	@Inject
	ViewModelProvider.Factory viewModelFactory;
	SetupViewModel viewModel;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);

		viewModel = new ViewModelProvider(this, viewModelFactory)
				.get(SetupViewModel.class);
		viewModel.getState().observeEvent(this, this::onStateChanged);
	}

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);
		// fade-in after splash screen instead of default animation
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		setContentView(R.layout.activity_fragment_container);
	}

	private void onStateChanged(SetupViewModel.State state) {
		if (state == AUTHOR_NAME) {
			setInputStateAlwaysVisible(this);
			showInitialFragment(AuthorNameFragment.newInstance());
		} else if (state == SET_PASSWORD) {
			setInputStateAlwaysVisible(this);
			showPasswordFragment();
		} else if (state == DOZE) {
			setInputStateHidden(this);
			showDozeFragment();
		} else if (state == CREATED || state == FAILED) {
			// TODO: Show an error if failed
			showApp();
		}
	}

	void showPasswordFragment() {
		showNextFragment(SetPasswordFragment.newInstance());
	}

	@TargetApi(23)
	void showDozeFragment() {
		showNextFragment(DozeFragment.newInstance());
	}

	void showApp() {
		Intent i = new Intent(this, ENTRY_ACTIVITY);
		i.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_TASK_ON_HOME |
				FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		supportFinishAfterTransition();
		overridePendingTransition(R.anim.screen_new_in, R.anim.screen_old_out);
	}

	@Override
	@Deprecated
	public void runOnDbThread(Runnable runnable) {
		throw new RuntimeException("Don't use this deprecated method here.");
	}

}

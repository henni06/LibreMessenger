package org.briarproject.briar.android.account;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;

import org.briarproject.bramble.api.account.AccountManager;
import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.activity.BaseActivity;
import org.briarproject.briar.android.fragment.BaseFragment.BaseFragmentListener;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_TASK_ON_HOME;
import static org.briarproject.briar.android.BriarApplication.ENTRY_ACTIVITY;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class SetupActivity extends BaseActivity
		implements BaseFragmentListener {

	private static final String STATE_KEY_AUTHOR_NAME = "authorName";
	private static final String STATE_KEY_PASSWORD = "password";

	@Inject
	AccountManager accountManager;

	@Inject
	SetupController setupController;

	@Nullable
	private String authorName, password;

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);
		// fade-in after splash screen instead of default animation
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		setContentView(R.layout.activity_fragment_container);

		if (state == null) {
			if (accountManager.accountExists()) throw new AssertionError();
			showInitialFragment(AuthorNameFragment.newInstance());
		} else {
			authorName = state.getString(STATE_KEY_AUTHOR_NAME);
			password = state.getString(STATE_KEY_PASSWORD);
		}
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
		setupController.setSetupActivity(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		if (authorName != null)
			state.putString(STATE_KEY_AUTHOR_NAME, authorName);
		if (password != null)
			state.putString(STATE_KEY_PASSWORD, password);
	}

	@Nullable
	String getAuthorName() {
		return authorName;
	}

	void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	@Nullable
	String getPassword() {
		return password;
	}

	void setPassword(String password) {
		this.password = password;
	}

	void showPasswordFragment() {
		if (authorName == null) throw new IllegalStateException();
		showNextFragment(SetPasswordFragment.newInstance());
	}

	@TargetApi(23)
	void showDozeFragment() {
		if (authorName == null) throw new IllegalStateException();
		if (password == null) throw new IllegalStateException();
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

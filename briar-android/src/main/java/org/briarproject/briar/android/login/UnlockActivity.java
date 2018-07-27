package org.briarproject.briar.android.login;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.widget.Button;

import org.briarproject.bramble.api.account.AccountManager;
import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.activity.BaseActivity;

import java.util.logging.Logger;

import javax.inject.Inject;

import static org.briarproject.briar.android.activity.RequestCodes.REQUEST_UNLOCK;

@RequiresApi(21)
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class UnlockActivity extends BaseActivity {

	private static final Logger LOG =
			Logger.getLogger(UnlockActivity.class.getSimpleName());

	@Inject
	AccountManager accountManager;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_unlock);

		Button button = findViewById(R.id.unlock);
		button.setOnClickListener(view -> requestKeyguardUnlock());
		requestKeyguardUnlock();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_UNLOCK && resultCode == RESULT_OK) {
			unlock();
		}
	}

	private void requestKeyguardUnlock() {
		KeyguardManager keyguardManager = (KeyguardManager) getSystemService(
				Context.KEYGUARD_SERVICE);
		assert keyguardManager != null;
		Intent intent = keyguardManager
				.createConfirmDeviceCredentialIntent(getString(R.string.lock_unlock),
						null);
		if (intent == null) {
			// the user must have removed the screen lock since locked
			LOG.warning("Unlocking without keyguard");
			unlock();
		} else {
			startActivityForResult(intent, REQUEST_UNLOCK);
		}
	}

	private void unlock() {
		accountManager.setLocked(false);
		setResult(RESULT_OK);
		ActivityCompat.finishAfterTransition(this);
	}

}

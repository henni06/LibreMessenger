package org.libreproject.libre.android;

import android.content.Intent;
import android.os.Bundle;

import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.libre.R;
import org.libreproject.libre.android.activity.ActivityComponent;
import org.libreproject.libre.android.activity.BaseActivity;
import org.libreproject.libre.android.fragment.BaseFragment.BaseFragmentListener;
import org.libreproject.libre.android.fragment.ErrorFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static org.libreproject.bramble.api.lifecycle.LifecycleManager.StartResult;
import static org.libreproject.libre.android.LibreService.EXTRA_START_RESULT;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class StartupFailureActivity extends BaseActivity implements
		BaseFragmentListener {

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);

		setContentView(R.layout.activity_fragment_container);
		handleIntent(getIntent());
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	private void handleIntent(Intent i) {
		StartResult result =
				(StartResult) i.getSerializableExtra(EXTRA_START_RESULT);

		// show proper error message
		int errorRes;
		switch (result) {
			case CLOCK_ERROR:
				errorRes = R.string.startup_failed_clock_error;
				break;
			case DATA_TOO_OLD_ERROR:
				errorRes = R.string.startup_failed_data_too_old_error;
				break;
			case DATA_TOO_NEW_ERROR:
				errorRes = R.string.startup_failed_data_too_new_error;
				break;
			case DB_ERROR:
				errorRes = R.string.startup_failed_db_error;
				break;
			case SERVICE_ERROR:
				errorRes = R.string.startup_failed_service_error;
				break;
			default:
				throw new IllegalArgumentException();
		}
		showInitialFragment(ErrorFragment.newInstance(getString(errorRes)));
	}

	@Override
	public void runOnDbThread(@NonNull Runnable runnable) {
		throw new UnsupportedOperationException();
	}
}

package org.briarproject.briar.android.hotspot;

import android.content.Intent;
import android.provider.Settings;

import org.briarproject.briar.R;

import java.util.logging.Logger;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.core.util.Consumer;

import static java.util.logging.Level.INFO;
import static java.util.logging.Logger.getLogger;

/**
 * This class ensures that the conditions to open a hotspot are fulfilled on
 * API levels < 29.
 * <p>
 * As soon as {@link #checkAndRequestConditions()} returns true,
 * all conditions are fulfilled.
 */
class ConditionManager extends AbstractConditionManager {

	private static final Logger LOG =
			getLogger(ConditionManager.class.getName());

	private final ActivityResultLauncher<Intent> wifiRequest;

	ConditionManager(ActivityResultCaller arc,
			Consumer<Boolean> permissionUpdateCallback) {
		super(permissionUpdateCallback);
		wifiRequest = arc.registerForActivityResult(
				new StartActivityForResult(),
				result -> permissionUpdateCallback
						.accept(wifiManager.isWifiEnabled()));
	}

	@Override
	void onStart() {
		// nothing to do here
	}

	private boolean areEssentialPermissionsGranted() {
		if (LOG.isLoggable(INFO)) {
			LOG.info(String.format("areEssentialPermissionsGranted(): " +
							"wifiManager.isWifiEnabled()? %b",
					wifiManager.isWifiEnabled()));
		}
		return wifiManager.isWifiEnabled();
	}

	@Override
	boolean checkAndRequestConditions() {
		if (areEssentialPermissionsGranted()) return true;

		if (!wifiManager.isWifiEnabled()) {
			// Try enabling the Wifi and return true if that seems to have been
			// successful, i.e. "Wifi is either already in the requested state, or
			// in progress toward the requested state".
			if (wifiManager.setWifiEnabled(true)) {
				LOG.info("Enabled wifi");
				return true;
			}

			// Wifi is not enabled and we can't seem to enable it, so ask the user
			// to enable it for us.
			showRationale(ctx, R.string.wifi_settings_title,
					R.string.wifi_settings_request_enable_body,
					this::requestEnableWiFi,
					() -> permissionUpdateCallback.accept(false));
		}

		return false;
	}

	private void requestEnableWiFi() {
		wifiRequest.launch(new Intent(Settings.ACTION_WIFI_SETTINGS));
	}

}

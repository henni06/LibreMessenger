package org.libreproject.bramble.account;

import android.app.Application;
import android.content.SharedPreferences;

import org.libreproject.bramble.api.crypto.CryptoComponent;
import org.libreproject.bramble.api.db.DatabaseConfig;
import org.libreproject.bramble.api.identity.IdentityManager;
import org.libreproject.libre.R;
import org.libreproject.libre.android.Localizer;
import org.libreproject.libre.android.util.UiUtils;

import javax.inject.Inject;

class LibreAccountManager extends AndroidAccountManager {

	@Inject
	LibreAccountManager(DatabaseConfig databaseConfig, CryptoComponent crypto,
			IdentityManager identityManager, SharedPreferences prefs,
			Application app) {
		super(databaseConfig, crypto, identityManager, prefs, app);
	}

	@Override
	public void deleteAccount() {
		synchronized (stateChangeLock) {
			super.deleteAccount();
			Localizer.reinitialize();
			UiUtils.setTheme(appContext,
					appContext.getString(R.string.pref_theme_light_value));
		}
	}
}

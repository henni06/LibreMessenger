package org.briarproject.bramble.account;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.briarproject.bramble.api.account.AccountManager;
import org.briarproject.bramble.api.db.DatabaseConfig;
import org.briarproject.bramble.util.IoUtils;

import java.io.File;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

class AndroidAccountManager extends AccountManagerImpl
		implements AccountManager {

	private static final Logger LOG =
			Logger.getLogger(AndroidAccountManager.class.getName());

	private static final String PREF_DB_KEY = "key";

	private final SharedPreferences briarPrefs;
	private final Context appContext;

	@Inject
	AndroidAccountManager(DatabaseConfig databaseConfig,
			SharedPreferences briarPrefs, Application app) {
		super(databaseConfig);
		this.briarPrefs = briarPrefs;
		appContext = app.getApplicationContext();
	}

	@Override
	@Nullable
	public String getEncryptedDatabaseKey() {
		String key = getDatabaseKeyFromPreferences();
		if (key == null) key = super.getEncryptedDatabaseKey();
		else migrateDatabaseKeyToFile(key);
		return key;
	}

	@Nullable
	private String getDatabaseKeyFromPreferences() {
		String key = briarPrefs.getString(PREF_DB_KEY, null);
		if (key == null) LOG.info("No database key in preferences");
		else LOG.info("Found database key in preferences");
		return key;
	}

	private void migrateDatabaseKeyToFile(String key) {
		if (storeEncryptedDatabaseKey(key)) {
			if (briarPrefs.edit().remove(PREF_DB_KEY).commit())
				LOG.info("Database key migrated to file");
			else LOG.warning("Database key not removed from preferences");
		} else {
			LOG.warning("Database key not migrated to file");
		}
	}

	@Override
	public void deleteAccount() {
		super.deleteAccount();
		SharedPreferences defaultPrefs =
				PreferenceManager.getDefaultSharedPreferences(appContext);
		deleteAppData(briarPrefs, defaultPrefs);
	}

	private void deleteAppData(SharedPreferences... clear) {
		// Clear and commit shared preferences
		for (SharedPreferences prefs : clear) {
			if (!prefs.edit().clear().commit())
				LOG.warning("Could not clear shared preferences");
		}
		// Delete files, except lib and shared_prefs directories
		File dataDir = new File(appContext.getApplicationInfo().dataDir);
		File[] children = dataDir.listFiles();
		if (children == null) {
			LOG.warning("Could not list files in app data dir");
		} else {
			for (File child : children) {
				String name = child.getName();
				if (!name.equals("lib") && !name.equals("shared_prefs")) {
					IoUtils.deleteFileOrDir(child);
				}
			}
		}
		// Recreate the cache dir as some OpenGL drivers expect it to exist
		if (!new File(dataDir, "cache").mkdir())
			LOG.warning("Could not recreate cache dir");
	}

}

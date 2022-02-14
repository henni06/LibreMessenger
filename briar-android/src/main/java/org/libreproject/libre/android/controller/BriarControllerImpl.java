package org.libreproject.libre.android.controller;

import android.app.Activity;
import android.content.Intent;
import android.os.IBinder;

import org.libreproject.bramble.api.account.AccountManager;
import org.libreproject.bramble.api.db.DatabaseExecutor;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.settings.Settings;
import org.libreproject.bramble.api.settings.SettingsManager;
import org.libreproject.bramble.api.system.AndroidWakeLockManager;
import org.libreproject.libre.android.LibreApplication;
import org.libreproject.libre.android.LibreService;
import org.libreproject.libre.android.LibreService.BriarServiceConnection;
import org.libreproject.libre.android.controller.handler.ResultHandler;
import org.libreproject.libre.api.android.DozeWatchdog;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.CallSuper;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.libreproject.bramble.api.lifecycle.LifecycleManager.LifecycleState.STARTING_SERVICES;
import static org.libreproject.bramble.util.LogUtils.logException;
import static org.libreproject.libre.android.settings.SettingsFragment.SETTINGS_NAMESPACE;
import static org.libreproject.libre.android.util.UiUtils.needsDozeWhitelisting;

@NotNullByDefault
public class BriarControllerImpl implements BriarController {

	private static final Logger LOG =
			getLogger(BriarControllerImpl.class.getName());

	public static final String DOZE_ASK_AGAIN = "dozeAskAgain";

	private final BriarServiceConnection serviceConnection;
	private final AccountManager accountManager;
	private final LifecycleManager lifecycleManager;
	private final Executor databaseExecutor;
	private final SettingsManager settingsManager;
	private final DozeWatchdog dozeWatchdog;
	private final AndroidWakeLockManager wakeLockManager;
	private final Activity activity;

	private boolean bound = false;

	@Inject
	BriarControllerImpl(BriarServiceConnection serviceConnection,
			AccountManager accountManager,
			LifecycleManager lifecycleManager,
			@DatabaseExecutor Executor databaseExecutor,
			SettingsManager settingsManager,
			DozeWatchdog dozeWatchdog,
			AndroidWakeLockManager wakeLockManager,
			Activity activity) {
		this.serviceConnection = serviceConnection;
		this.accountManager = accountManager;
		this.lifecycleManager = lifecycleManager;
		this.databaseExecutor = databaseExecutor;
		this.settingsManager = settingsManager;
		this.dozeWatchdog = dozeWatchdog;
		this.wakeLockManager = wakeLockManager;
		this.activity = activity;
	}

	@Override
	@CallSuper
	public void onActivityCreate(Activity activity) {
		if (accountManager.hasDatabaseKey()) startAndBindService();
	}

	@Override
	public void onActivityStart() {
	}

	@Override
	public void onActivityStop() {
	}

	@Override
	@CallSuper
	public void onActivityDestroy() {
		unbindService();
	}

	@Override
	public void startAndBindService() {
		activity.startService(new Intent(activity, LibreService.class));
		bound = activity.bindService(new Intent(activity, LibreService.class),
				serviceConnection, 0);
	}

	@Override
	public boolean accountSignedIn() {
		return accountManager.hasDatabaseKey() &&
				lifecycleManager.getLifecycleState().isAfter(STARTING_SERVICES);
	}

	@Override
	public void hasDozed(ResultHandler<Boolean> handler) {
		LibreApplication app = (LibreApplication) activity.getApplication();
		if (app.isInstrumentationTest() || !dozeWatchdog.getAndResetDozeFlag()
				|| !needsDozeWhitelisting(activity)) {
			handler.onResult(false);
			return;
		}
		databaseExecutor.execute(() -> {
			try {
				Settings settings =
						settingsManager.getSettings(SETTINGS_NAMESPACE);
				boolean ask = settings.getBoolean(DOZE_ASK_AGAIN, true);
				handler.onResult(ask);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	@Override
	public void doNotAskAgainForDozeWhiteListing() {
		databaseExecutor.execute(() -> {
			try {
				Settings settings = new Settings();
				settings.putBoolean(DOZE_ASK_AGAIN, false);
				settingsManager.mergeSettings(settings, SETTINGS_NAMESPACE);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	@Override
	public void signOut(ResultHandler<Void> handler, boolean deleteAccount) {
		wakeLockManager.executeWakefully(() -> {
			try {
				// Wait for the service to finish starting up
				IBinder binder = serviceConnection.waitForBinder();
				LibreService service =
						((LibreService.BriarBinder) binder).getService();
				service.waitForStartup();
				// Shut down the service and wait for it to shut down
				LOG.info("Shutting down service");
				service.shutdown();
				service.waitForShutdown();
			} catch (InterruptedException e) {
				LOG.warning("Interrupted while waiting for service");
			} finally {
				if (deleteAccount) accountManager.deleteAccount();
			}
			handler.onResult(null);
		}, "SignOut");
	}

	@Override
	public void deleteAccount() {
		accountManager.deleteAccount();
	}

	private void unbindService() {
		if (bound) activity.unbindService(serviceConnection);
	}

}

package org.briarproject.briar.android.navdrawer;

import android.app.Application;

import org.briarproject.bramble.api.db.DatabaseExecutor;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.settings.Settings;
import org.briarproject.bramble.api.settings.SettingsManager;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.UiThread;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.briarproject.bramble.util.LogUtils.logException;
import static org.briarproject.briar.android.TestingConstants.EXPIRY_DATE;
import static org.briarproject.briar.android.controller.BriarControllerImpl.DOZE_ASK_AGAIN;
import static org.briarproject.briar.android.settings.SettingsFragment.SETTINGS_NAMESPACE;
import static org.briarproject.briar.android.util.UiUtils.needsDozeWhitelisting;

@NotNullByDefault
public class NavDrawerViewModel extends AndroidViewModel {

	private static final Logger LOG =
			getLogger(NavDrawerViewModel.class.getName());

	private static final String EXPIRY_DATE_WARNING = "expiryDateWarning";

	@DatabaseExecutor
	private final Executor dbExecutor;
	private final SettingsManager settingsManager;

	private final MutableLiveData<Boolean> showExpiryWarning =
			new MutableLiveData<>();
	private final MutableLiveData<Boolean> shouldAskForDozeWhitelisting =
			new MutableLiveData<>();

	@Inject
	NavDrawerViewModel(Application app, @DatabaseExecutor Executor dbExecutor,
			SettingsManager settingsManager) {
		super(app);
		this.dbExecutor = dbExecutor;
		this.settingsManager = settingsManager;
	}

	LiveData<Boolean> showExpiryWarning() {
		return showExpiryWarning;
	}

	@UiThread
	void checkExpiryWarning() {
		dbExecutor.execute(() -> {
			try {
				Settings settings =
						settingsManager.getSettings(SETTINGS_NAMESPACE);
				int warningInt = settings.getInt(EXPIRY_DATE_WARNING, 0);

				if (warningInt == 0) {
					// we have not warned before
					showExpiryWarning.postValue(true);
				} else {
					long warningLong = warningInt * 1000L;
					long now = System.currentTimeMillis();
					long daysSinceLastWarning =
							(now - warningLong) / DAYS.toMillis(1);
					long daysBeforeExpiry =
							(EXPIRY_DATE - now) / DAYS.toMillis(1);

					if (daysSinceLastWarning >= 30) {
						showExpiryWarning.postValue(true);
					} else if (daysBeforeExpiry <= 3 &&
							daysSinceLastWarning > 0) {
						showExpiryWarning.postValue(true);
					} else {
						showExpiryWarning.postValue(false);
					}
				}
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	@UiThread
	void expiryWarningDismissed() {
		showExpiryWarning.setValue(false);
		dbExecutor.execute(() -> {
			try {
				Settings settings = new Settings();
				int date = (int) (System.currentTimeMillis() / 1000L);
				settings.putInt(EXPIRY_DATE_WARNING, date);
				settingsManager.mergeSettings(settings, SETTINGS_NAMESPACE);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	LiveData<Boolean> shouldAskForDozeWhitelisting() {
		return shouldAskForDozeWhitelisting;
	}

	@UiThread
	void checkDozeWhitelisting() {
		// check this first, to hit the DbThread only when really necessary
		if (!needsDozeWhitelisting(getApplication())) {
			shouldAskForDozeWhitelisting.setValue(false);
			return;
		}
		dbExecutor.execute(() -> {
			try {
				Settings settings =
						settingsManager.getSettings(SETTINGS_NAMESPACE);
				boolean ask = settings.getBoolean(DOZE_ASK_AGAIN, true);
				shouldAskForDozeWhitelisting.postValue(ask);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				shouldAskForDozeWhitelisting.postValue(true);
			}
		});
	}
}

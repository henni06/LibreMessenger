package org.briarproject.briar.android;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.VmPolicy;
import android.preference.PreferenceManager;

import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;

import org.briarproject.bramble.BrambleAndroidEagerSingletons;
import org.briarproject.bramble.BrambleAppComponent;
import org.briarproject.bramble.BrambleCoreEagerSingletons;
import org.briarproject.briar.BriarCoreEagerSingletons;
import org.briarproject.briar.R;
import org.briarproject.briar.android.logging.CachingLogHandler;
import org.briarproject.briar.android.reporting.BriarExceptionHandler;
import org.briarproject.briar.android.util.UiUtils;

import java.util.Collection;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import androidx.annotation.NonNull;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Logger.getLogger;
import static org.briarproject.briar.android.TestingConstants.IS_DEBUG_BUILD;

public class BriarApplicationImpl extends Application
		implements BriarApplication {

	private static final Logger LOG =
			getLogger(BriarApplicationImpl.class.getName());

	private final CachingLogHandler logHandler = new CachingLogHandler();
	private final BriarExceptionHandler exceptionHandler =
			new BriarExceptionHandler(this);

	private AndroidComponent applicationComponent;
	private volatile SharedPreferences prefs;

	@Override
	protected void attachBaseContext(Context base) {
		Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
		if (prefs == null)
			prefs = PreferenceManager.getDefaultSharedPreferences(base);
		// Loading the language needs to be done here.
		Localizer.initialize(prefs);
		super.attachBaseContext(
				Localizer.getInstance().setLocale(base));
		setTheme(base, prefs);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		if (IS_DEBUG_BUILD) enableStrictMode();

		Logger rootLogger = getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		// Disable the Android logger for release builds
		for (Handler handler : handlers) rootLogger.removeHandler(handler);
		if (IS_DEBUG_BUILD) {
			// We can't set the level of the Android logger at runtime, so
			// raise records to the logger's default level
			rootLogger.addHandler(new LevelRaisingHandler(FINE, INFO));
			// Restore the default handlers after the level raising handler
			for (Handler handler : handlers) rootLogger.addHandler(handler);
		}
		rootLogger.addHandler(logHandler);
		rootLogger.setLevel(IS_DEBUG_BUILD ? FINE : INFO);

		LOG.info("Created");

		applicationComponent = createApplicationComponent();
		EmojiManager.install(new GoogleEmojiProvider());
	}

	protected AndroidComponent createApplicationComponent() {
		AndroidComponent androidComponent = DaggerAndroidComponent.builder()
				.appModule(new AppModule(this))
				.build();

		// We need to load the eager singletons directly after making the
		// dependency graphs
		BrambleCoreEagerSingletons.Helper
				.injectEagerSingletons(androidComponent);
		BrambleAndroidEagerSingletons.Helper
				.injectEagerSingletons(androidComponent);
		BriarCoreEagerSingletons.Helper.injectEagerSingletons(androidComponent);
		AndroidEagerSingletons.Helper.injectEagerSingletons(androidComponent);
		return androidComponent;
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Localizer.getInstance().setLocale(this);
	}

	private void setTheme(Context ctx, SharedPreferences prefs) {
		String theme = prefs.getString("pref_key_theme", null);
		if (theme == null) {
			// set default value
			theme = getString(R.string.pref_theme_light_value);
			prefs.edit().putString("pref_key_theme", theme).apply();
		}
		// set theme
		UiUtils.setTheme(ctx, theme);
	}

	private void enableStrictMode() {
		ThreadPolicy.Builder threadPolicy = new ThreadPolicy.Builder();
		threadPolicy.detectAll();
		threadPolicy.penaltyLog();
		StrictMode.setThreadPolicy(threadPolicy.build());
		VmPolicy.Builder vmPolicy = new VmPolicy.Builder();
		vmPolicy.detectAll();
		vmPolicy.penaltyLog();
		StrictMode.setVmPolicy(vmPolicy.build());
	}

	@Override
	public BrambleAppComponent getBrambleAppComponent() {
		return applicationComponent;
	}

	@Override
	public Collection<LogRecord> getRecentLogRecords() {
		return logHandler.getRecentLogRecords();
	}

	@Override
	public AndroidComponent getApplicationComponent() {
		return applicationComponent;
	}

	@Override
	public SharedPreferences getDefaultSharedPreferences() {
		return prefs;
	}

	@Override
	public boolean isRunningInBackground() {
		RunningAppProcessInfo info = new RunningAppProcessInfo();
		ActivityManager.getMyMemoryState(info);
		return (info.importance != IMPORTANCE_FOREGROUND);
	}
}

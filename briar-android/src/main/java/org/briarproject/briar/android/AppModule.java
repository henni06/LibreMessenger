package org.briarproject.briar.android;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.StrictMode;

import com.vanniktech.emoji.RecentEmoji;

import org.briarproject.bramble.api.FeatureFlags;
import org.briarproject.bramble.api.crypto.CryptoComponent;
import org.briarproject.bramble.api.crypto.KeyStrengthener;
import org.briarproject.bramble.api.crypto.PublicKey;
import org.briarproject.bramble.api.db.DatabaseConfig;
import org.briarproject.bramble.api.event.EventBus;
import org.briarproject.bramble.api.lifecycle.LifecycleManager;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.plugin.BluetoothConstants;
import org.briarproject.bramble.api.plugin.LanTcpConstants;
import org.briarproject.bramble.api.plugin.PluginConfig;
import org.briarproject.bramble.api.plugin.TorDirectory;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.plugin.duplex.DuplexPluginFactory;
import org.briarproject.bramble.api.plugin.simplex.SimplexPluginFactory;
import org.briarproject.bramble.api.reporting.DevConfig;
import org.briarproject.bramble.plugin.bluetooth.AndroidBluetoothPluginFactory;
import org.briarproject.bramble.plugin.tcp.AndroidLanTcpPluginFactory;
import org.briarproject.bramble.plugin.tor.AndroidTorPluginFactory;
import org.briarproject.bramble.util.AndroidUtils;
import org.briarproject.bramble.util.StringUtils;
import org.briarproject.briar.android.account.LockManagerImpl;
import org.briarproject.briar.android.contact.ContactListModule;
import org.briarproject.briar.android.forum.ForumModule;
import org.briarproject.briar.android.keyagreement.ContactExchangeModule;
import org.briarproject.briar.android.login.LoginModule;
import org.briarproject.briar.android.navdrawer.NavDrawerModule;
import org.briarproject.briar.android.privategroup.list.GroupListModule;
import org.briarproject.briar.android.reporting.DevReportModule;
import org.briarproject.briar.android.viewmodel.ViewModelModule;
import org.briarproject.briar.api.android.AndroidNotificationManager;
import org.briarproject.briar.api.android.DozeWatchdog;
import org.briarproject.briar.api.android.LockManager;
import org.briarproject.briar.api.android.ScreenFilterMonitor;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static android.content.Context.MODE_PRIVATE;
import static android.os.Build.VERSION.SDK_INT;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.briarproject.bramble.api.reporting.ReportingConstants.DEV_ONION_ADDRESS;
import static org.briarproject.bramble.api.reporting.ReportingConstants.DEV_PUBLIC_KEY_HEX;
import static org.briarproject.briar.android.TestingConstants.IS_DEBUG_BUILD;

@Module(includes = {
		ContactExchangeModule.class,
		LoginModule.class,
		NavDrawerModule.class,
		ViewModelModule.class,
		DevReportModule.class,
		ContactListModule.class,
		// below need to be within same scope as ViewModelProvider.Factory
		ForumModule.BindsModule.class,
		GroupListModule.class,
})
public class AppModule {

	static class EagerSingletons {
		@Inject
		AndroidNotificationManager androidNotificationManager;
		@Inject
		ScreenFilterMonitor screenFilterMonitor;
		@Inject
		NetworkUsageLogger networkUsageLogger;
		@Inject
		DozeWatchdog dozeWatchdog;
		@Inject
		LockManager lockManager;
		@Inject
		RecentEmoji recentEmoji;
	}

	private final Application application;

	public AppModule(Application application) {
		this.application = application;
	}

	@Provides
	@Singleton
	Application providesApplication() {
		return application;
	}

	@Provides
	@Singleton
	DatabaseConfig provideDatabaseConfig(Application app) {
		//FIXME: StrictMode
		StrictMode.ThreadPolicy tp = StrictMode.allowThreadDiskReads();
		StrictMode.allowThreadDiskWrites();
		File dbDir = app.getApplicationContext().getDir("db", MODE_PRIVATE);
		File keyDir = app.getApplicationContext().getDir("key", MODE_PRIVATE);
		StrictMode.setThreadPolicy(tp);
		KeyStrengthener keyStrengthener = SDK_INT >= 23
				? new AndroidKeyStrengthener() : null;
		return new AndroidDatabaseConfig(dbDir, keyDir, keyStrengthener);
	}

	@Provides
	@Singleton
	@TorDirectory
	File provideTorDirectory(Application app) {
		return app.getDir("tor", MODE_PRIVATE);
	}

	@Provides
	PluginConfig providePluginConfig(AndroidBluetoothPluginFactory bluetooth,
			AndroidTorPluginFactory tor, AndroidLanTcpPluginFactory lan) {
		@NotNullByDefault
		PluginConfig pluginConfig = new PluginConfig() {

			@Override
			public Collection<DuplexPluginFactory> getDuplexFactories() {
				return asList(bluetooth, tor, lan);
			}

			@Override
			public Collection<SimplexPluginFactory> getSimplexFactories() {
				return emptyList();
			}

			@Override
			public boolean shouldPoll() {
				return true;
			}

			@Override
			public Map<TransportId, List<TransportId>> getTransportPreferences() {
				// Prefer LAN to Bluetooth
				return singletonMap(BluetoothConstants.ID,
						singletonList(LanTcpConstants.ID));
			}
		};
		return pluginConfig;
	}

	@Provides
	@Singleton
	DevConfig provideDevConfig(Application app, CryptoComponent crypto) {
		@NotNullByDefault
		DevConfig devConfig = new DevConfig() {

			@Override
			public PublicKey getDevPublicKey() {
				try {
					return crypto.getMessageKeyParser().parsePublicKey(
							StringUtils.fromHexString(DEV_PUBLIC_KEY_HEX));
				} catch (GeneralSecurityException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public String getDevOnionAddress() {
				return DEV_ONION_ADDRESS;
			}

			@Override
			public File getReportDir() {
				return AndroidUtils.getReportDir(app.getApplicationContext());
			}
		};
		return devConfig;
	}

	@Provides
	SharedPreferences provideSharedPreferences(Application app) {
		// FIXME unify this with getDefaultSharedPreferences()
		return app.getSharedPreferences("db", MODE_PRIVATE);
	}

	@Provides
	@Singleton
	AndroidNotificationManager provideAndroidNotificationManager(
			LifecycleManager lifecycleManager, EventBus eventBus,
			AndroidNotificationManagerImpl notificationManager) {
		lifecycleManager.registerService(notificationManager);
		eventBus.addListener(notificationManager);
		return notificationManager;
	}

	@Provides
	@Singleton
	ScreenFilterMonitor provideScreenFilterMonitor(
			LifecycleManager lifecycleManager,
			ScreenFilterMonitorImpl screenFilterMonitor) {
		if (SDK_INT <= 29) {
			// this keeps track of installed apps and does not work on API 30+
			lifecycleManager.registerService(screenFilterMonitor);
		}
		return screenFilterMonitor;
	}

	@Provides
	NetworkUsageLogger provideNetworkUsageLogger(
			LifecycleManager lifecycleManager) {
		NetworkUsageLogger networkUsageLogger = new NetworkUsageLogger();
		lifecycleManager.registerService(networkUsageLogger);
		return networkUsageLogger;
	}

	@Provides
	@Singleton
	DozeWatchdog provideDozeWatchdog(LifecycleManager lifecycleManager) {
		DozeWatchdogImpl dozeWatchdog = new DozeWatchdogImpl(application);
		lifecycleManager.registerService(dozeWatchdog);
		return dozeWatchdog;
	}

	@Provides
	@Singleton
	LockManager provideLockManager(LifecycleManager lifecycleManager,
			EventBus eventBus, LockManagerImpl lockManager) {
		lifecycleManager.registerService(lockManager);
		eventBus.addListener(lockManager);
		return lockManager;
	}

	@Provides
	@Singleton
	RecentEmoji provideRecentEmoji(LifecycleManager lifecycleManager,
			RecentEmojiImpl recentEmoji) {
		lifecycleManager.registerOpenDatabaseHook(recentEmoji);
		return recentEmoji;
	}

	@Provides
	FeatureFlags provideFeatureFlags() {
		return () -> IS_DEBUG_BUILD;
	}
}

package org.briarproject.briar.android.settings;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.plugin.BluetoothConstants;
import org.briarproject.bramble.api.plugin.LanTcpConstants;
import org.briarproject.bramble.api.plugin.TorConstants;
import org.briarproject.bramble.api.settings.Settings;
import org.briarproject.bramble.api.settings.SettingsManager;

import java.util.concurrent.Executor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static org.briarproject.bramble.api.plugin.Plugin.PREF_PLUGIN_ENABLE;
import static org.briarproject.bramble.api.plugin.TorConstants.DEFAULT_PREF_TOR_MOBILE;
import static org.briarproject.bramble.api.plugin.TorConstants.DEFAULT_PREF_TOR_NETWORK;
import static org.briarproject.bramble.api.plugin.TorConstants.DEFAULT_PREF_TOR_ONLY_WHEN_CHARGING;
import static org.briarproject.bramble.api.plugin.TorConstants.PREF_TOR_MOBILE;
import static org.briarproject.bramble.api.plugin.TorConstants.PREF_TOR_NETWORK;
import static org.briarproject.bramble.api.plugin.TorConstants.PREF_TOR_NETWORK_NEVER;
import static org.briarproject.bramble.api.plugin.TorConstants.PREF_TOR_ONLY_WHEN_CHARGING;
import static org.briarproject.briar.android.settings.SettingsViewModel.BT_NAMESPACE;
import static org.briarproject.briar.android.settings.SettingsViewModel.TOR_NAMESPACE;
import static org.briarproject.briar.android.settings.SettingsViewModel.WIFI_NAMESPACE;

@NotNullByDefault
class ConnectionsManager {

	final ConnectionsStore btStore;
	final ConnectionsStore wifiStore;
	final ConnectionsStore torStore;

	private final MutableLiveData<Boolean> btEnabled = new MutableLiveData<>();
	private final MutableLiveData<Boolean> wifiEnabled =
			new MutableLiveData<>();
	private final MutableLiveData<Boolean> torEnabled = new MutableLiveData<>();
	private final MutableLiveData<String> torNetwork = new MutableLiveData<>();
	private final MutableLiveData<Boolean> torMobile = new MutableLiveData<>();
	private final MutableLiveData<Boolean> torCharging =
			new MutableLiveData<>();

	ConnectionsManager(SettingsManager settingsManager,
			Executor dbExecutor) {
		btStore =
				new ConnectionsStore(settingsManager, dbExecutor, BT_NAMESPACE);
		wifiStore = new ConnectionsStore(settingsManager, dbExecutor,
				WIFI_NAMESPACE);
		torStore = new ConnectionsStore(settingsManager, dbExecutor,
				TOR_NAMESPACE);
	}

	void updateBtSetting(Settings btSettings) {
		btEnabled.postValue(btSettings.getBoolean(PREF_PLUGIN_ENABLE,
				BluetoothConstants.DEFAULT_PREF_PLUGIN_ENABLE));
	}

	void updateWifiSettings(Settings wifiSettings) {
		wifiEnabled.postValue(wifiSettings.getBoolean(PREF_PLUGIN_ENABLE,
				LanTcpConstants.DEFAULT_PREF_PLUGIN_ENABLE));
	}

	void updateTorSettings(Settings settings) {
		Settings torSettings = migrateTorSettings(settings);
		torEnabled.postValue(torSettings.getBoolean(PREF_PLUGIN_ENABLE,
				TorConstants.DEFAULT_PREF_PLUGIN_ENABLE));

		int torNetworkSetting = torSettings.getInt(PREF_TOR_NETWORK,
				DEFAULT_PREF_TOR_NETWORK);
		torNetwork.postValue(Integer.toString(torNetworkSetting));

		torMobile.postValue(torSettings.getBoolean(PREF_TOR_MOBILE,
				DEFAULT_PREF_TOR_MOBILE));
		torCharging
				.postValue(torSettings.getBoolean(PREF_TOR_ONLY_WHEN_CHARGING,
						DEFAULT_PREF_TOR_ONLY_WHEN_CHARGING));
	}

	// TODO: Remove after a reasonable migration period (added 2020-06-25)
	private Settings migrateTorSettings(Settings s) {
		int network = s.getInt(PREF_TOR_NETWORK, DEFAULT_PREF_TOR_NETWORK);
		if (network == PREF_TOR_NETWORK_NEVER) {
			s.putInt(PREF_TOR_NETWORK, DEFAULT_PREF_TOR_NETWORK);
			s.putBoolean(PREF_PLUGIN_ENABLE, false);
			// We don't need to save the migrated settings - the Tor plugin is
			// responsible for that. This code just handles the case where the
			// settings are loaded before the plugin migrates them.
		}
		return s;
	}

	LiveData<Boolean> btEnabled() {
		return btEnabled;
	}

	LiveData<Boolean> wifiEnabled() {
		return wifiEnabled;
	}

	LiveData<Boolean> torEnabled() {
		return torEnabled;
	}

	LiveData<String> torNetwork() {
		return torNetwork;
	}

	LiveData<Boolean> torMobile() {
		return torMobile;
	}

	LiveData<Boolean> torCharging() {
		return torCharging;
	}

}

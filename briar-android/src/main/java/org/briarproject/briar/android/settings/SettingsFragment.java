package org.briarproject.briar.android.settings;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.acra.ACRA;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.event.Event;
import org.briarproject.bramble.api.event.EventBus;
import org.briarproject.bramble.api.event.EventListener;
import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.bramble.api.settings.Settings;
import org.briarproject.bramble.api.settings.SettingsManager;
import org.briarproject.bramble.api.settings.event.SettingsUpdatedEvent;
import org.briarproject.bramble.api.system.AndroidExecutor;
import org.briarproject.bramble.util.StringUtils;
import org.briarproject.briar.R;
import org.briarproject.briar.android.util.UserFeedback;
import org.briarproject.briar.android.widget.PreferenceDividerDecoration;

import java.util.logging.Logger;

import static android.app.Activity.RESULT_OK;
import static android.media.RingtoneManager.ACTION_RINGTONE_PICKER;
import static android.media.RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI;
import static android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI;
import static android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI;
import static android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT;
import static android.media.RingtoneManager.EXTRA_RINGTONE_TITLE;
import static android.media.RingtoneManager.EXTRA_RINGTONE_TYPE;
import static android.media.RingtoneManager.TYPE_NOTIFICATION;
import static android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static org.briarproject.briar.android.activity.RequestCodes.REQUEST_RINGTONE;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class SettingsFragment extends PreferenceFragmentCompat
		implements EventListener, Preference.OnPreferenceChangeListener {

	public static final String SETTINGS_NAMESPACE = "android-ui";
	public static final String PREF_NOTIFY_GROUP = "notifyGroupMessages";
	public static final String PREF_NOTIFY_BLOG = "notifyBlogPosts";

	private static final Logger LOG =
			Logger.getLogger(SettingsFragment.class.getName());

	private SettingsActivity listener;
	private AndroidExecutor androidExecutor;
	private ListPreference enableBluetooth;
	private ListPreference torOverMobile;
	private CheckBoxPreference notifyPrivateMessages;
	private CheckBoxPreference notifyGroupMessages;
	private CheckBoxPreference notifyForumPosts;
	private CheckBoxPreference notifyBlogPosts;
	private CheckBoxPreference notifyVibration;
	private Preference notifySound;

	// Fields that are accessed from background threads must be volatile
	private volatile SettingsManager settingsManager;
	private volatile EventBus eventBus;
	private volatile Settings settings;
	private volatile boolean bluetoothSetting = false, torSetting = false;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		listener = (SettingsActivity) context;
		androidExecutor = listener.getAndroidExecutor();
		settingsManager = listener.getSettingsManager();
		eventBus = listener.getEventBus();
	}

	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.settings);

		enableBluetooth =
				(ListPreference) findPreference("pref_key_bluetooth");
		torOverMobile =
				(ListPreference) findPreference("pref_key_tor_mobile");
		notifyPrivateMessages = (CheckBoxPreference) findPreference(
				"pref_key_notify_private_messages");
		notifyGroupMessages = (CheckBoxPreference) findPreference(
				"pref_key_notify_group_messages");
		notifyForumPosts = (CheckBoxPreference) findPreference(
				"pref_key_notify_forum_posts");
		notifyBlogPosts = (CheckBoxPreference) findPreference(
				"pref_key_notify_blog_posts");
		notifyVibration = (CheckBoxPreference) findPreference(
				"pref_key_notify_vibration");
		notifySound = findPreference("pref_key_notify_sound");

		enableBluetooth.setOnPreferenceChangeListener(this);
		torOverMobile.setOnPreferenceChangeListener(this);
		notifyPrivateMessages.setOnPreferenceChangeListener(this);
		notifyGroupMessages.setOnPreferenceChangeListener(this);
		notifyForumPosts.setOnPreferenceChangeListener(this);
		notifyBlogPosts.setOnPreferenceChangeListener(this);
		notifyVibration.setOnPreferenceChangeListener(this);

		notifySound.setOnPreferenceClickListener(
				new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						String title =
								getString(R.string.choose_ringtone_title);
						Intent i = new Intent(ACTION_RINGTONE_PICKER);
						i.putExtra(EXTRA_RINGTONE_TYPE, TYPE_NOTIFICATION);
						i.putExtra(EXTRA_RINGTONE_TITLE, title);
						i.putExtra(EXTRA_RINGTONE_DEFAULT_URI,
								DEFAULT_NOTIFICATION_URI);
						i.putExtra(EXTRA_RINGTONE_SHOW_SILENT, true);
						if (settings.getBoolean("notifySound", true)) {
							Uri uri;
							String ringtoneUri =
									settings.get("notifyRingtoneUri");
							if (StringUtils.isNullOrEmpty(ringtoneUri))
								uri = DEFAULT_NOTIFICATION_URI;
							else uri = Uri.parse(ringtoneUri);
							i.putExtra(EXTRA_RINGTONE_EXISTING_URI, uri);
						}
						startActivityForResult(i, REQUEST_RINGTONE);
						return true;
					}
				});

		findPreference("pref_key_send_feedback").setOnPreferenceClickListener(
				new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						triggerFeedback();
						return true;
					}
				});

		loadSettings();
	}

	@Override
	public RecyclerView onCreateRecyclerView(LayoutInflater inflater,
			ViewGroup parent, Bundle savedInstanceState) {
		RecyclerView list = super.onCreateRecyclerView(inflater, parent,
				savedInstanceState);
		list.addItemDecoration(
				new PreferenceDividerDecoration(getContext()).drawBottom(true));
		return list;
	}

	@Override
	public void onStart() {
		super.onStart();
		eventBus.addListener(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		eventBus.removeListener(this);
	}

	private void loadSettings() {
		listener.runOnDbThread(new Runnable() {
			@Override
			public void run() {
				try {
					long now = System.currentTimeMillis();
					settings = settingsManager.getSettings(SETTINGS_NAMESPACE);
					Settings btSettings = settingsManager.getSettings("bt");
					Settings torSettings = settingsManager.getSettings("tor");
					long duration = System.currentTimeMillis() - now;
					if (LOG.isLoggable(INFO))
						LOG.info("Loading settings took " + duration + " ms");
					bluetoothSetting = btSettings.getBoolean("enable", false);
					torSetting = torSettings.getBoolean("torOverMobile", true);
					displaySettings();
				} catch (DbException e) {
					if (LOG.isLoggable(WARNING))
						LOG.log(WARNING, e.toString(), e);
				}
			}
		});
	}

	private void displaySettings() {
		listener.runOnUiThreadUnlessDestroyed(new Runnable() {
			@Override
			public void run() {
				enableBluetooth.setValue(Boolean.toString(bluetoothSetting));
				torOverMobile.setValue(Boolean.toString(torSetting));

				notifyPrivateMessages.setChecked(settings.getBoolean(
						"notifyPrivateMessages", true));

				notifyGroupMessages.setChecked(settings.getBoolean(
						PREF_NOTIFY_GROUP, true));

				notifyForumPosts.setChecked(settings.getBoolean(
						"notifyForumPosts", true));

				notifyBlogPosts.setChecked(settings.getBoolean(
						PREF_NOTIFY_BLOG, true));

				notifyVibration.setChecked(settings.getBoolean(
						"notifyVibration", true));

				String text;
				if (settings.getBoolean("notifySound", true)) {
					String ringtoneName = settings.get("notifyRingtoneName");
					if (StringUtils.isNullOrEmpty(ringtoneName)) {
						text = getString(R.string.notify_sound_setting_default);
					} else {
						text = ringtoneName;
					}
				} else {
					text = getString(R.string.notify_sound_setting_disabled);
				}
				notifySound.setSummary(text);
			}
		});
	}

	private void triggerFeedback() {
		androidExecutor.runOnBackgroundThread(new Runnable() {
			@Override
			public void run() {
				ACRA.getErrorReporter().handleException(new UserFeedback(),
						false);
			}
		});
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object o) {
		if (preference == enableBluetooth) {
			bluetoothSetting = Boolean.valueOf((String) o);
			enableOrDisableBluetooth(bluetoothSetting);
			storeBluetoothSettings();
		} else if (preference == torOverMobile) {
			torSetting = Boolean.valueOf((String) o);
			storeTorSettings();
		} else if (preference == notifyPrivateMessages) {
			Settings s = new Settings();
			s.putBoolean("notifyPrivateMessages", (Boolean) o);
			storeSettings(s);
		} else if (preference == notifyGroupMessages) {
			Settings s = new Settings();
			s.putBoolean(PREF_NOTIFY_GROUP, (Boolean) o);
			storeSettings(s);
		} else if (preference == notifyForumPosts) {
			Settings s = new Settings();
			s.putBoolean("notifyForumPosts", (Boolean) o);
			storeSettings(s);
		} else if (preference == notifyBlogPosts) {
			Settings s = new Settings();
			s.putBoolean(PREF_NOTIFY_BLOG, (Boolean) o);
			storeSettings(s);
		} else if (preference == notifyVibration) {
			Settings s = new Settings();
			s.putBoolean("notifyVibration", (Boolean) o);
			storeSettings(s);
		}
		return true;
	}

	private void enableOrDisableBluetooth(final boolean enable) {
		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter != null) {
			androidExecutor.runOnBackgroundThread(new Runnable() {
				@Override
				public void run() {
					if (enable) adapter.enable();
					else adapter.disable();
				}
			});
		}
	}

	private void storeTorSettings() {
		listener.runOnDbThread(new Runnable() {
			@Override
			public void run() {
				try {
					Settings s = new Settings();
					s.putBoolean("torOverMobile", torSetting);
					long now = System.currentTimeMillis();
					settingsManager.mergeSettings(s, "tor");
					long duration = System.currentTimeMillis() - now;
					if (LOG.isLoggable(INFO))
						LOG.info("Merging settings took " + duration + " ms");
				} catch (DbException e) {
					if (LOG.isLoggable(WARNING))
						LOG.log(WARNING, e.toString(), e);
				}
			}
		});
	}

	private void storeBluetoothSettings() {
		listener.runOnDbThread(new Runnable() {
			@Override
			public void run() {
				try {
					Settings s = new Settings();
					s.putBoolean("enable", bluetoothSetting);
					long now = System.currentTimeMillis();
					settingsManager.mergeSettings(s, "bt");
					long duration = System.currentTimeMillis() - now;
					if (LOG.isLoggable(INFO))
						LOG.info("Merging settings took " + duration + " ms");
				} catch (DbException e) {
					if (LOG.isLoggable(WARNING))
						LOG.log(WARNING, e.toString(), e);
				}
			}
		});
	}

	private void storeSettings(final Settings settings) {
		listener.runOnDbThread(new Runnable() {
			@Override
			public void run() {
				try {
					long now = System.currentTimeMillis();
					settingsManager.mergeSettings(settings, SETTINGS_NAMESPACE);
					long duration = System.currentTimeMillis() - now;
					if (LOG.isLoggable(INFO))
						LOG.info("Merging settings took " + duration + " ms");
				} catch (DbException e) {
					if (LOG.isLoggable(WARNING))
						LOG.log(WARNING, e.toString(), e);
				}
			}
		});
	}

	@Override
	public void onActivityResult(int request, int result, Intent data) {
		super.onActivityResult(request, result, data);
		if (request == REQUEST_RINGTONE && result == RESULT_OK) {
			Settings s = new Settings();
			Uri uri = data.getParcelableExtra(EXTRA_RINGTONE_PICKED_URI);
			if (uri == null) {
				// The user chose silence
				s.putBoolean("notifySound", false);
				s.put("notifyRingtoneName", "");
				s.put("notifyRingtoneUri", "");
			} else if (RingtoneManager.isDefault(uri)) {
				// The user chose the default
				s.putBoolean("notifySound", true);
				s.put("notifyRingtoneName", "");
				s.put("notifyRingtoneUri", "");
			} else {
				// The user chose a ringtone other than the default
				Ringtone r = RingtoneManager.getRingtone(getContext(), uri);
				String name = r.getTitle(getContext());
				s.putBoolean("notifySound", true);
				s.put("notifyRingtoneName", name);
				s.put("notifyRingtoneUri", uri.toString());
			}
			storeSettings(s);
		}
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof SettingsUpdatedEvent) {
			String namespace = ((SettingsUpdatedEvent) e).getNamespace();
			if (namespace.equals("bt") || namespace.equals("tor")
					|| namespace.equals(SETTINGS_NAMESPACE)) {
				LOG.info("Settings updated");
				loadSettings();
			}
		}
	}
}

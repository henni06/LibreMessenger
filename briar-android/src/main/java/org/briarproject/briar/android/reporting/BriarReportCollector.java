/*
  Some of the code in this file was copied from or inspired by ACRA
  which is licenced under Apache 2.0 and authored by F43nd1r.
  https://github.com/ACRA/acra/blob/3b9034/acra-core/src/main/java/org/acra/collector/
 */

package org.briarproject.briar.android.reporting;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;

import org.briarproject.bramble.api.Pair;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.briar.BuildConfig;
import org.briarproject.briar.R;
import org.briarproject.briar.android.BriarApplication;
import org.briarproject.briar.android.logging.BriefLogFormatter;
import org.briarproject.briar.android.reporting.ReportData.MultiReportInfo;
import org.briarproject.briar.android.reporting.ReportData.ReportItem;
import org.briarproject.briar.android.reporting.ReportData.SingleReportInfo;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import javax.annotation.concurrent.Immutable;

import androidx.annotation.Nullable;

import static android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE;
import static android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
import static android.content.Context.WIFI_P2P_SERVICE;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;
import static android.os.Build.VERSION.SDK_INT;
import static androidx.core.content.ContextCompat.getSystemService;
import static java.util.Locale.US;
import static java.util.Objects.requireNonNull;
import static java.util.TimeZone.getTimeZone;
import static org.briarproject.bramble.util.AndroidUtils.getBluetoothAddressAndMethod;
import static org.briarproject.bramble.util.PrivacyUtils.scrubInetAddress;
import static org.briarproject.bramble.util.PrivacyUtils.scrubMacAddress;
import static org.briarproject.bramble.util.StringUtils.isNullOrEmpty;

@Immutable
@NotNullByDefault
class BriarReportCollector {

	private final Context ctx;

	BriarReportCollector(Context ctx) {
		this.ctx = ctx;
	}

	public ReportData collectReportData(@Nullable Throwable t,
			long appStartTime) {
		ReportData reportData = new ReportData()
				.add(getBasicInfo(t))
				.add(getDeviceInfo());
		if (t != null) reportData.add(getStacktrace(t));
		return reportData
				.add(getTimeInfo(appStartTime))
				.add(getMemory())
				.add(getStorage())
				.add(getConnectivity())
				.add(getBuildConfig())
				.add(getLogcat())
				.add(getDeviceFeatures());
	}

	private ReportItem getBasicInfo(@Nullable Throwable t) {
		String packageName = ctx.getPackageName();
		PackageManager pm = ctx.getPackageManager();
		String versionName;
		int versionCode;
		try {
			PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
			versionName = packageInfo.versionName;
			versionCode = packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			versionName = e.toString();
			versionCode = 0;
		}
		MultiReportInfo basicInfo = new MultiReportInfo()
				.add("PackageName", packageName)
				.add("VersionName", versionName)
				.add("VersionCode", versionCode)
				.add("IsCrashReport", t != null);
		return new ReportItem("BasicInfo", R.string.dev_report_basic_info,
				basicInfo, false);
	}

	private ReportItem getDeviceInfo() {
		MultiReportInfo deviceInfo = new MultiReportInfo()
				.add("AndroidVersion", Build.VERSION.RELEASE)
				.add("AndroidApi", SDK_INT)
				.add("Product", Build.PRODUCT)
				.add("Model", Build.MODEL)
				.add("Brand", Build.BRAND);
		return new ReportItem("DeviceInfo", R.string.dev_report_device_info,
				deviceInfo);
	}

	private ReportItem getStacktrace(Throwable t) {
		final Writer sw = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(sw);
		if (!isNullOrEmpty(t.getMessage())) {
			printWriter.println(t.getMessage());
		}
		t.printStackTrace(printWriter);
		SingleReportInfo stacktrace = new SingleReportInfo(sw.toString());
		return new ReportItem("Stacktrace", R.string.dev_report_stacktrace,
				stacktrace);
	}

	private ReportItem getTimeInfo(long startTime) {
		MultiReportInfo timeInfo = new MultiReportInfo()
				.add("ReportTime", formatTime(System.currentTimeMillis()));
		if (startTime > -1) {
			timeInfo.add("AppStartTime", formatTime(startTime));
		}
		return new ReportItem("TimeInfo", R.string.dev_report_time_info,
				timeInfo);
	}

	private String formatTime(long time) {
		SimpleDateFormat format =
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", US);
		format.setTimeZone(getTimeZone("UTC"));
		return format.format(new Date(time));
	}

	private ReportItem getMemory() {
		MultiReportInfo memInfo = new MultiReportInfo();

		// System memory
		ActivityManager am = getSystemService(ctx, ActivityManager.class);
		ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
		requireNonNull(am).getMemoryInfo(mem);
		memInfo.add("SystemMemoryTotal", mem.totalMem);
		memInfo.add("SystemMemoryFree", mem.availMem);
		memInfo.add("SystemMemoryThreshold", mem.threshold);

		// Virtual machine memory
		Runtime runtime = Runtime.getRuntime();
		memInfo.add("VirtualMachineMemoryAllocated", runtime.totalMemory());
		memInfo.add("VirtualMachineMemoryFree", runtime.freeMemory());
		memInfo.add("VirtualMachineMemoryMaximum", runtime.maxMemory());

		return new ReportItem("Memory", R.string.dev_report_memory, memInfo);
	}

	private ReportItem getStorage() {
		MultiReportInfo storageInfo = new MultiReportInfo();

		// Internal storage
		File root = Environment.getRootDirectory();
		storageInfo.add("InternalStorageTotal", root.getTotalSpace());
		storageInfo.add("InternalStorageFree", root.getFreeSpace());

		// External storage (SD card)
		File sd = Environment.getExternalStorageDirectory();
		storageInfo.add("ExternalStorageTotal", sd.getTotalSpace());
		storageInfo.add("ExternalStorageFree", sd.getFreeSpace());

		return new ReportItem("Storage", R.string.dev_report_storage,
				storageInfo);
	}

	private ReportItem getConnectivity() {
		MultiReportInfo connectivityInfo = new MultiReportInfo();

		// Is mobile data available?
		ConnectivityManager cm = requireNonNull(
				getSystemService(ctx, ConnectivityManager.class));
		NetworkInfo mobile = cm.getNetworkInfo(TYPE_MOBILE);
		boolean mobileAvailable = mobile != null && mobile.isAvailable();
		connectivityInfo.add("MobileDataAvailable", mobileAvailable);

		// Is mobile data enabled?
		boolean mobileEnabled = false;
		try {
			Class<?> clazz = Class.forName(cm.getClass().getName());
			Method method = clazz.getDeclaredMethod("getMobileDataEnabled");
			method.setAccessible(true);
			mobileEnabled = (Boolean) requireNonNull(method.invoke(cm));
		} catch (ClassNotFoundException
				| NoSuchMethodException
				| IllegalArgumentException
				| InvocationTargetException
				| IllegalAccessException e) {
			connectivityInfo
					.add("MobileDataReflectionException", e.toString());
		}
		connectivityInfo.add("MobileDataEnabled", mobileEnabled);

		// Is mobile data connected ?
		boolean mobileConnected = mobile != null && mobile.isConnected();
		connectivityInfo.add("MobileDataConnected", mobileConnected);

		// Is wifi available?
		NetworkInfo wifi = cm.getNetworkInfo(TYPE_WIFI);
		boolean wifiAvailable = wifi != null && wifi.isAvailable();
		connectivityInfo.add("WifiAvailable", wifiAvailable);

		// Is wifi enabled?
		WifiManager wm = getSystemService(ctx, WifiManager.class);
		boolean wifiEnabled = wm != null &&
				wm.getWifiState() == WIFI_STATE_ENABLED;
		connectivityInfo.add("WifiEnabled", wifiEnabled);

		// Is wifi connected?
		boolean wifiConnected = wifi != null && wifi.isConnected();
		connectivityInfo.add("WifiConnected", wifiConnected);

		// Is wifi direct supported?
		boolean wifiDirect = ctx.getSystemService(WIFI_P2P_SERVICE) != null;
		connectivityInfo.add("WiFiDirectSupported", wifiDirect);

		if (wm != null) {
			WifiInfo wifiInfo = wm.getConnectionInfo();
			if (wifiInfo != null) {
				int ip = wifiInfo.getIpAddress(); // Nice API, Google
				byte[] ipBytes = new byte[4];
				ipBytes[0] = (byte) (ip & 0xFF);
				ipBytes[1] = (byte) ((ip >> 8) & 0xFF);
				ipBytes[2] = (byte) ((ip >> 16) & 0xFF);
				ipBytes[3] = (byte) ((ip >> 24) & 0xFF);
				try {
					InetAddress address = InetAddress.getByAddress(ipBytes);
					connectivityInfo.add("WiFiAddress",
							scrubInetAddress(address));
				} catch (UnknownHostException ignored) {
					// Should only be thrown if address has illegal length
				}
			}
		}

		// Is Bluetooth available?
		BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
		if (bt == null) {
			connectivityInfo.add("BluetoothAvailable", false);
		} else {
			connectivityInfo.add("BluetoothAvailable", true);

			// Is Bluetooth enabled?
			@SuppressLint("HardwareIds")
			boolean btEnabled = bt.isEnabled()
					&& !isNullOrEmpty(bt.getAddress());
			connectivityInfo.add("BluetoothEnabled", btEnabled);

			// Is Bluetooth connectable?
			int scanMode = bt.getScanMode();
			boolean btConnectable = scanMode == SCAN_MODE_CONNECTABLE ||
					scanMode == SCAN_MODE_CONNECTABLE_DISCOVERABLE;
			connectivityInfo.add("BluetoothConnectable", btConnectable);

			// Is Bluetooth discoverable?
			boolean btDiscoverable =
					scanMode == SCAN_MODE_CONNECTABLE_DISCOVERABLE;
			connectivityInfo.add("BluetoothDiscoverable", btDiscoverable);

			if (SDK_INT >= 21) {
				// Is Bluetooth LE scanning and advertising supported?
				boolean btLeScan = bt.getBluetoothLeScanner() != null;
				connectivityInfo.add("BluetoothLeScanningSupported", btLeScan);
				boolean btLeAdvertise =
						bt.getBluetoothLeAdvertiser() != null;
				connectivityInfo.add("BluetoothLeAdvertisingSupported",
						btLeAdvertise);
			}

			Pair<String, String> p = getBluetoothAddressAndMethod(ctx, bt);
			String address = p.getFirst();
			String method = p.getSecond();
			connectivityInfo.add("BluetoothAddress", scrubMacAddress(address));
			connectivityInfo.add("BluetoothAddressMethod", method);
		}
		return new ReportItem("Connectivity", R.string.dev_report_connectivity,
				connectivityInfo);
	}

	private ReportItem getBuildConfig() {
		MultiReportInfo buildConfig = new MultiReportInfo()
				.add("GitHash", BuildConfig.GitHash)
				.add("BuildType", BuildConfig.BUILD_TYPE)
				.add("Flavor", BuildConfig.FLAVOR)
				.add("Debug", BuildConfig.DEBUG)
				.add("BuildTimestamp", formatTime(BuildConfig.BuildTimestamp));
		return new ReportItem("BuildConfig", R.string.dev_report_build_config,
				buildConfig);
	}

	private ReportItem getLogcat() {
		BriarApplication app = (BriarApplication) ctx.getApplicationContext();
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new BriefLogFormatter();
		for (LogRecord record : app.getRecentLogRecords()) {
			sb.append(formatter.format(record)).append('\n');
		}
		return new ReportItem("Logcat", R.string.dev_report_logcat,
				sb.toString());
	}

	private ReportItem getDeviceFeatures() {
		PackageManager pm = ctx.getPackageManager();
		FeatureInfo[] features = pm.getSystemAvailableFeatures();
		MultiReportInfo deviceFeatures = new MultiReportInfo();
		for (FeatureInfo feature : features) {
			String featureName = feature.name;
			if (featureName != null) {
				deviceFeatures.add(featureName, true);
			} else {
				deviceFeatures.add("glEsVersion", feature.getGlEsVersion());
			}
		}
		return new ReportItem("DeviceFeatures",
				R.string.dev_report_device_features, deviceFeatures);
	}

}

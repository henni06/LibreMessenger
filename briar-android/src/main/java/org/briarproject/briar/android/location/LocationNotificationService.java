package org.briarproject.briar.android.location;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;

import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.BriarActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class LocationNotificationService extends Service {
	public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";

	public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";

	LocationRequest mLocationRequest;
	GoogleApiClient mGoogleApiClient;
	public static Location mCurrentLocation;


	@SuppressLint("MissingPermission")
	@Override
	public void onCreate() {

		LocationManager mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(true);
		criteria.setCostAllowed(true);
		android.location.LocationListener listener=new android.location.LocationListener() {
			@Override
			public void onLocationChanged(@NonNull Location location) {
				Intent intent = new Intent("locationChanged");
				intent.putExtra("lng", location.getLongitude());
				intent.putExtra("lat", location.getLatitude());
				intent.putExtra("bearing", location.getBearing());
				LocalBroadcastManager.getInstance(LocationNotificationService.this).sendBroadcast(intent);
			}
		};
		String provider = mLocationManager.getBestProvider(criteria, true);

		mLocationManager.requestLocationUpdates(provider, 10000, 10,
				listener);
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			String action = intent.getAction();

			switch (action) {
				case ACTION_START_FOREGROUND_SERVICE:
					processStartService();
					break;
				case ACTION_STOP_FOREGROUND_SERVICE:

					stopForegroundService();
					break;
			}
		}

		return START_STICKY;
	}

	private void stopForegroundService(){
		Log.d(LocationNotificationService.class.getName(), "Stop foreground service.");

		// Stop foreground service and remove the notification.
		stopForeground(true);

		// Stop the foreground service.
		stopSelf();
	}

	private void processStartService(){
		String NOTIFICATION_CHANNEL_ID = "com.freiheitsmessenger";
		String channelName = getString(R.string.app_name);
		NotificationChannel chan = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
		}

		NotificationManager manager = (NotificationManager) getSystemService(
				Context.NOTIFICATION_SERVICE);
		assert manager != null;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			manager.createNotificationChannel(chan);
		}
		Intent notificationIntent = new Intent(this, BriarActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		Notification notification = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			notification = new Notification.Builder(this,NOTIFICATION_CHANNEL_ID)
					.setSmallIcon(R.drawable.ic_menu_mylocation)
					.setContentTitle(getString(R.string.sharing_warning))
					.setContentIntent(pendingIntent).build();
		}

		startForeground(1337, notification);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopForeground(true);
	}
}
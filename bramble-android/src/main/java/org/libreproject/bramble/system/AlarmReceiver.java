package org.libreproject.bramble.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.libreproject.bramble.BrambleApplication;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		BrambleApplication app =
				(BrambleApplication) ctx.getApplicationContext();
		app.getBrambleAppComponent().alarmListener().onAlarm(intent);
	}
}

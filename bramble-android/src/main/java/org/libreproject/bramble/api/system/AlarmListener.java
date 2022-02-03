package org.libreproject.bramble.api.system;

import android.content.Intent;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface AlarmListener {

	void onAlarm(Intent intent);
}

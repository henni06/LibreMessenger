package org.libreproject.libre.android.util;

import android.content.Context;

import org.libreproject.libre.R;

import androidx.annotation.ColorRes;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import static android.os.Build.VERSION.SDK_INT;
import static androidx.core.app.NotificationCompat.VISIBILITY_PRIVATE;

public class LibreNotificationBuilder extends NotificationCompat.Builder {

	private final Context context;

	public LibreNotificationBuilder(Context context, String channelId) {
		super(context, channelId);
		this.context = context;
		// Auto-cancel does not fire the delete intent, see
		// https://issuetracker.google.com/issues/36961721
		setAutoCancel(true);

		setLights(ContextCompat.getColor(context, R.color.libre_lime_400),
				750, 500);
		if (SDK_INT >= 21) setVisibility(VISIBILITY_PRIVATE);
	}

	public LibreNotificationBuilder setColorRes(@ColorRes int res) {
		setColor(ContextCompat.getColor(context, res));
		return this;
	}

	public LibreNotificationBuilder setNotificationCategory(String category) {
		if (SDK_INT >= 21) setCategory(category);
		return this;
	}

}

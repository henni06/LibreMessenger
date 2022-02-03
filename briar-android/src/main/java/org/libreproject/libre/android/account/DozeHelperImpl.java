package org.libreproject.libre.android.account;

import android.content.Context;

import static org.libreproject.libre.android.util.UiUtils.needsDozeWhitelisting;

class DozeHelperImpl implements DozeHelper {
	@Override
	public boolean needToShowDozeFragment(Context context) {
		Context appContext = context.getApplicationContext();
		return needsDozeWhitelisting(appContext) ||
				HuaweiProtectedAppsView.needsToBeShown(appContext) ||
				HuaweiAppLaunchView.needsToBeShown(appContext) ||
				XiaomiView.isXiaomiOrRedmiDevice();
	}
}

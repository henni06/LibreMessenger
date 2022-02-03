package org.libreproject.libre.android;

import android.app.Activity;
import android.content.SharedPreferences;

import org.libreproject.bramble.BrambleApplication;
import org.libreproject.libre.android.navdrawer.NavDrawerActivity;

/**
 * This exists so that the Application object will not necessarily be cast
 * directly to the Briar application object.
 */
public interface BriarApplication extends BrambleApplication {

	Class<? extends Activity> ENTRY_ACTIVITY = NavDrawerActivity.class;

	AndroidComponent getApplicationComponent();

	SharedPreferences getDefaultSharedPreferences();

	boolean isRunningInBackground();

	boolean isInstrumentationTest();
}

package org.briarproject.briar.android;

import android.app.Activity;
import android.util.Log;

import org.briarproject.bramble.api.connection.ConnectionRegistry;
import org.briarproject.bramble.api.system.Clock;
import org.briarproject.briar.api.test.TestDataCreator;
import org.junit.ClassRule;

import javax.inject.Inject;

import tools.fastlane.screengrab.FalconScreenshotStrategy;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

public abstract class ScreenshotTest extends UiTest {

	@ClassRule
	public static final LocaleTestRule localeTestRule = new LocaleTestRule();

	@Inject
	protected TestDataCreator testDataCreator;
	@Inject
	protected ConnectionRegistry connectionRegistry;
	@Inject
	protected Clock clock;

	protected void screenshot(String name, Activity activity) {
		try {
			Screengrab.screenshot(name, new FalconScreenshotStrategy(activity));
		} catch (RuntimeException e) {
			if (e.getMessage() == null ||
					!e.getMessage().equals("Unable to capture screenshot."))
				throw e;
			// The tests should still pass when run from AndroidStudio
			// without manually granting permissions like fastlane does.
			Log.w("Screengrab", "Permission to write screenshot is missing.");
		}
	}

	protected long getMinutesAgo(int minutes) {
		return clock.currentTimeMillis() - minutes * 60 * 1000;
	}

}

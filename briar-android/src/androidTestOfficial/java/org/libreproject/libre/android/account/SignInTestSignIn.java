package org.libreproject.libre.android.account;

import android.view.Gravity;

import org.libreproject.libre.R;
import org.libreproject.libre.android.BriarUiTestComponent;
import org.libreproject.libre.android.UiTest;
import org.libreproject.libre.android.login.StartupActivity;
import org.libreproject.libre.android.navdrawer.NavDrawerActivity;
import org.libreproject.libre.android.splash.SplashScreenActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerMatchers.isClosed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.libreproject.libre.android.ViewActions.waitFor;
import static org.hamcrest.CoreMatchers.allOf;

/**
 * This relies on class sorting to run after {@link SignInTestCreateAccount}.
 */
@RunWith(AndroidJUnit4.class)
public class SignInTestSignIn extends UiTest {

	@Override
	protected void inject(BriarUiTestComponent component) {
		component.inject(this);
	}

	@Test
	public void signIn() throws Exception {
		startActivity(SplashScreenActivity.class);

		waitFor(StartupActivity.class);

		// enter password
		onView(withId(R.id.edit_password))
				.check(matches(isDisplayed()))
				.perform(replaceText(PASSWORD));
		onView(withId(R.id.btn_sign_in))
				.check(matches(allOf(isDisplayed(), isEnabled())))
				.perform(click());

		lifecycleManager.waitForStartup();
		waitFor(NavDrawerActivity.class);

		// ensure nav drawer is visible
		onView(withId(R.id.drawer_layout))
				.check(matches(isClosed(Gravity.START)));
	}
}

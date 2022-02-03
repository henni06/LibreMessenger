package org.libreproject.libre.android.account;

import android.view.Gravity;

import org.libreproject.libre.R;
import org.libreproject.libre.android.BriarUiTestComponent;
import org.libreproject.libre.android.UiTest;
import org.libreproject.libre.android.navdrawer.NavDrawerActivity;
import org.libreproject.libre.android.splash.SplashScreenActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerMatchers.isClosed;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.libreproject.libre.android.ViewActions.waitFor;
import static org.libreproject.libre.android.ViewActions.waitUntilMatches;
import static org.hamcrest.Matchers.endsWith;

@RunWith(AndroidJUnit4.class)
public class SignInTestCreateAccount extends UiTest {

	@Override
	protected void inject(BriarUiTestComponent component) {
		component.inject(this);
	}

	@Test
	public void createAccount() throws Exception {
		accountManager.deleteAccount();
		accountManager.createAccount(USERNAME, PASSWORD);

		startActivity(SplashScreenActivity.class);
		lifecycleManager.waitForStartup();
		waitFor(NavDrawerActivity.class);

		// open nav drawer
		onView(withId(R.id.drawer_layout))
				.check(matches(isClosed(Gravity.START)))
				.perform(DrawerActions.open());

		// click onboarding away (once shown)
		onView(isRoot()).perform(waitUntilMatches(hasDescendant(
				withClassName(endsWith("PromptView")))));
		onView(withClassName(endsWith("PromptView")))
				.perform(click());

		// sign-out manually
		onView(withText(R.string.sign_out_button))
				.check(matches(isDisplayed()))
				.perform(click());
		lifecycleManager.waitForShutdown();
	}

}

package org.briarproject.briar.android;

import android.content.Context;

import org.briarproject.bramble.api.contact.Contact;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.briar.R;
import org.briarproject.briar.android.account.SetupActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.briarproject.bramble.api.plugin.LanTcpConstants.ID;
import static org.briarproject.briar.android.ViewActions.waitUntilMatches;
import static org.briarproject.briar.android.util.UiUtils.needsDozeWhitelisting;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SetupDataTest extends ScreenshotTest {

	@Rule
	public IntentsTestRule<SetupActivity> testRule =
			new IntentsTestRule<SetupActivity>(SetupActivity.class) {
				@Override
				protected void beforeActivityLaunched() {
					super.beforeActivityLaunched();
					accountManager.deleteAccount();
				}
			};

	@Override
	protected void inject(BriarUiTestComponent component) {
		component.inject(this);
	}

	@Test
	public void createAccount() throws Exception {
		// Enter username
		onView(withText(R.string.setup_title))
				.check(matches(isDisplayed()));
		onView(withId(R.id.nickname_entry))
				.check(matches(isDisplayed()))
				.perform(typeText(USERNAME));
		onView(withId(R.id.nickname_entry))
				.perform(waitUntilMatches(withText(USERNAME)));

		screenshot("manual_create_account", testRule.getActivity());

		onView(withId(R.id.next))
				.check(matches(isDisplayed()))
				.perform(click());

		// Enter password
		onView(withId(R.id.password_entry))
				.check(matches(isDisplayed()))
				.perform(typeText(PASSWORD));
		onView(withId(R.id.password_confirm))
				.check(matches(isDisplayed()))
				.perform(typeText(PASSWORD));
		onView(withId(R.id.next))
				.check(matches(isDisplayed()))
				.perform(click());

		// White-list Doze if needed
		if (needsDozeWhitelisting(getApplicationContext())) {
			onView(withText(R.string.setup_doze_button))
					.check(matches(isDisplayed()))
					.perform(click());
			UiDevice device = UiDevice.getInstance(getInstrumentation());
			UiObject allowButton = device.findObject(
					new UiSelector().className("android.widget.Button")
							.index(1));
			allowButton.click();
			onView(withId(R.id.next))
					.check(matches(isDisplayed()))
					.perform(click());
		}

		lifecycleManager.waitForStartup();
		assertTrue(accountManager.hasDatabaseKey());
		createTestData();

		// close expiry warning
		onView(withId(R.id.expiryWarning))
				.perform(waitUntilMatches(isDisplayed()));
		onView(withId(R.id.expiryWarningClose))
				.check(matches(isDisplayed()));
		onView(withId(R.id.expiryWarningClose))
				.perform(click());
	}

	private void createTestData() {
		try {
			createTestDataExceptions();
		} catch (DbException e) {
			throw new AssertionError(e);
		}
	}

	private void createTestDataExceptions()
			throws DbException {
		Context ctx = getApplicationContext();
		String bobName = ctx.getString(R.string.screenshot_bob);
		Contact bob = testDataCreator.addContact(bobName, true);

		// TODO add messages

		connectionRegistry.registerIncomingConnection(bob.getId(), ID, () -> {
		});
	}

}

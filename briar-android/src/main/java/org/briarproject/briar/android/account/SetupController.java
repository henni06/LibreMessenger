package org.briarproject.briar.android.account;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface SetupController {

	void setSetupActivity(SetupActivity setupActivity);

	boolean needToShowDozeFragment();

	void setAuthorName(String authorName);

	float estimatePasswordStrength(String password);

	void setPassword(String password);

	/**
	 * This should be called after the author name has been set.
	 */
	void showPasswordFragment();

	/**
	 * This should be called after the author name and the password have been
	 * set.
	 */
	void showDozeFragment();

	/**
	 * This should be called after the author name and the password have been
	 * set.
	 */
	void createAccount();
}

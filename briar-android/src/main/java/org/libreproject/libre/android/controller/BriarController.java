package org.libreproject.libre.android.controller;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.system.Wakeful;
import org.libreproject.libre.android.controller.handler.ResultHandler;

@NotNullByDefault
public interface BriarController extends ActivityLifecycleController {

	void startAndBindService();

	boolean accountSignedIn();

	/**
	 * Returns true via the handler when the app has dozed
	 * without being white-listed.
	 */
	void hasDozed(ResultHandler<Boolean> handler);

	void doNotAskAgainForDozeWhiteListing();

	@Wakeful
	void signOut(ResultHandler<Void> handler, boolean deleteAccount);

	void deleteAccount();

}

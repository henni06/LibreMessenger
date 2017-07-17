package org.briarproject.briar.android;

/**
 * This exists so that the Application object will not necessarily be cast
 * directly to the Briar application object.
 */
public interface BriarApplication {

	// This build expires on 15 October 2017
	long EXPIRY_DATE = 1508022000 * 1000L;

	AndroidComponent getApplicationComponent();

}

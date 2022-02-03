package org.libreproject.libre.api.forum;

import org.libreproject.bramble.api.sync.ClientId;
import org.libreproject.libre.api.sharing.SharingManager;

public interface ForumSharingManager extends SharingManager<Forum> {

	/**
	 * The unique ID of the forum sharing client.
	 */
	ClientId CLIENT_ID = new ClientId("org.briarproject.briar.forum.sharing");

	/**
	 * The current major version of the forum sharing client.
	 */
	int MAJOR_VERSION = 0;

	/**
	 * The current minor version of the forum sharing client.
	 */
	int MINOR_VERSION = 1;
}

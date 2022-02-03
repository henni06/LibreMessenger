package org.libreproject.libre.api.sharing;

import static org.libreproject.bramble.api.sync.SyncConstants.MAX_MESSAGE_BODY_LENGTH;

public interface SharingConstants {

	/**
	 * The maximum length of an invitation's optional text in UTF-8 bytes.
	 */
	int MAX_INVITATION_TEXT_LENGTH = MAX_MESSAGE_BODY_LENGTH - 1024;

}

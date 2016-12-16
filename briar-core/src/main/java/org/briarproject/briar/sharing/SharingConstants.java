package org.briarproject.briar.sharing;

import org.briarproject.briar.client.MessageTrackerConstants;

interface SharingConstants {

	// Group metadata keys
	String GROUP_KEY_CONTACT_ID = "contactId";

	// Message metadata keys
	String MSG_KEY_MESSAGE_TYPE = "messageType";
	String MSG_KEY_PRIVATE_GROUP_ID = "privateGroupId";
	String MSG_KEY_TIMESTAMP = "timestamp";
	String MSG_KEY_READ = MessageTrackerConstants.MSG_KEY_READ;
	String MSG_KEY_LOCAL = "local";
	String MSG_KEY_VISIBLE_IN_UI = "visibleInUi";
	String MSG_KEY_AVAILABLE_TO_ANSWER = "availableToAnswer";

	// Session keys
	String SESSION_KEY_SESSION_ID = "sessionId";
	String SESSION_KEY_GROUP_ID = "groupId";
	String SESSION_KEY_LAST_LOCAL_MESSAGE_ID = "lastLocalMessageId";
	String SESSION_KEY_LAST_REMOTE_MESSAGE_ID = "lastRemoteMessageId";
	String SESSION_KEY_LOCAL_TIMESTAMP = "localTimestamp";
	String SESSION_KEY_INVITE_TIMESTAMP = "inviteTimestamp";
	String SESSION_KEY_ROLE = "role";
	String SESSION_KEY_STATE = "state";

}

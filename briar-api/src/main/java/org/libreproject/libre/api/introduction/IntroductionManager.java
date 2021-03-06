package org.libreproject.libre.api.introduction;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.ClientId;
import org.libreproject.libre.api.client.SessionId;
import org.libreproject.libre.api.conversation.ConversationManager.ConversationClient;

import javax.annotation.Nullable;

@NotNullByDefault
public interface IntroductionManager extends ConversationClient {

	/**
	 * The unique ID of the introduction client.
	 */
	ClientId CLIENT_ID = new ClientId("org.briarproject.briar.introduction");

	/**
	 * The current major version of the introduction client.
	 */
	int MAJOR_VERSION = 1;

	/**
	 * Returns true if both contacts can be introduced at this moment.
	 */
	boolean canIntroduce(Contact c1, Contact c2) throws DbException;

	/**
	 * The current minor version of the introduction client.
	 */
	int MINOR_VERSION = 1;

	/**
	 * Sends two initial introduction messages.
	 */
	void makeIntroduction(Contact c1, Contact c2, @Nullable String text)
			throws DbException;

	/**
	 * Responds to an introduction.
	 */
	void respondToIntroduction(ContactId contactId, SessionId sessionId,
			boolean accept) throws DbException;

}

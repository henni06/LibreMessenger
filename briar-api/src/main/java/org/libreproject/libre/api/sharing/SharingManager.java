package org.libreproject.libre.api.sharing;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.api.client.SessionId;
import org.libreproject.libre.api.conversation.ConversationManager.ConversationClient;

import java.util.Collection;

import javax.annotation.Nullable;

@NotNullByDefault
public interface SharingManager<S extends Shareable>
		extends ConversationClient {

	/**
	 * Sends an invitation to share the given group with the given contact,
	 * including optional text.
	 */
	void sendInvitation(GroupId shareableId, ContactId contactId,
			@Nullable String text) throws DbException;

	/**
	 * Responds to a pending group invitation
	 */
	void respondToInvitation(S s, Contact c, boolean accept)
			throws DbException;

	/**
	 * Responds to a pending group invitation
	 */
	void respondToInvitation(ContactId c, SessionId id, boolean accept)
			throws DbException;

	/**
	 * Returns all invitations to groups.
	 */
	Collection<SharingInvitationItem> getInvitations() throws DbException;

	/**
	 * Returns all contacts with whom the given group is shared.
	 */
	Collection<Contact> getSharedWith(GroupId g) throws DbException;

	/**
	 * Returns all contacts with whom the given group is shared.
	 */
	Collection<Contact> getSharedWith(Transaction txn, GroupId g)
			throws DbException;

	/**
	 * Returns true if the group not already shared and no invitation is open
	 */
	boolean canBeShared(GroupId g, Contact c) throws DbException;

}

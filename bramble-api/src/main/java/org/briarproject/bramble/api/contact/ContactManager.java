package org.briarproject.bramble.api.contact;

import org.briarproject.bramble.api.crypto.SecretKey;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.Transaction;
import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.identity.AuthorId;
import org.briarproject.bramble.api.identity.AuthorInfo;
import org.briarproject.bramble.api.lifecycle.LifecycleManager;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;

import java.util.Collection;

import javax.annotation.Nullable;

@NotNullByDefault
public interface ContactManager {

	/**
	 * Registers a hook to be called whenever a contact is added or removed.
	 * This method should be called before
	 * {@link LifecycleManager#startServices(SecretKey)}.
	 */
	void registerContactHook(ContactHook hook);

	/**
	 * Stores a contact associated with the given local and remote pseudonyms,
	 * derives and stores transport keys for each transport, and returns an ID
	 * for the contact.
	 *
	 * @param alice true if the local party is Alice
	 */
	ContactId addContact(Transaction txn, Author remote, AuthorId local,
			SecretKey rootKey, long timestamp, boolean alice, boolean verified,
			boolean active) throws DbException;

	/**
	 * Stores a contact associated with the given local and remote pseudonyms
	 * and returns an ID for the contact.
	 */
	ContactId addContact(Transaction txn, Author remote, AuthorId local,
			boolean verified, boolean active) throws DbException;

	/**
	 * Stores a contact associated with the given local and remote pseudonyms,
	 * derives and stores transport keys for each transport, and returns an ID
	 * for the contact.
	 *
	 * @param alice true if the local party is Alice
	 */
	ContactId addContact(Author remote, AuthorId local, SecretKey rootKey,
			long timestamp, boolean alice, boolean verified, boolean active)
			throws DbException;

	/**
	 * Returns the static link that needs to be sent to the contact to be added.
	 */
	String getRemoteContactLink();

	/**
	 * Returns true if the given link is syntactically valid.
	 */
	boolean isValidRemoteContactLink(String link);

	/**
	 * Requests a new contact to be added via the given {@code link}.
	 *
	 * @param link The link received from the contact we want to add.
	 * @param alias The alias the user has given this contact.
	 * @return A PendingContact representing the contact to be added.
	 */
	PendingContact addRemoteContactRequest(String link, String alias);

	/**
	 * Returns a list of {@link PendingContact}s.
	 */
	Collection<PendingContact> getPendingContacts();

	/**
	 * Removes a {@link PendingContact} that is in state
	 * {@link PendingContactState FAILED}.
	 */
	void removePendingContact(PendingContact pendingContact);

	/**
	 * Returns the contact with the given ID.
	 */
	Contact getContact(ContactId c) throws DbException;

	/**
	 * Returns the contact with the given remoteAuthorId
	 * that was added by the LocalAuthor with the given localAuthorId
	 *
	 * @throws org.briarproject.bramble.api.db.NoSuchContactException
	 */
	Contact getContact(AuthorId remoteAuthorId, AuthorId localAuthorId)
			throws DbException;

	/**
	 * Returns the contact with the given remoteAuthorId
	 * that was added by the LocalAuthor with the given localAuthorId
	 *
	 * @throws org.briarproject.bramble.api.db.NoSuchContactException
	 */
	Contact getContact(Transaction txn, AuthorId remoteAuthorId,
			AuthorId localAuthorId) throws DbException;

	/**
	 * Returns all active contacts.
	 */
	Collection<Contact> getActiveContacts() throws DbException;

	/**
	 * Removes a contact and all associated state.
	 */
	void removeContact(ContactId c) throws DbException;

	/**
	 * Removes a contact and all associated state.
	 */
	void removeContact(Transaction txn, ContactId c) throws DbException;

	/**
	 * Marks a contact as active or inactive.
	 */
	void setContactActive(Transaction txn, ContactId c, boolean active)
			throws DbException;

	/**
	 * Sets an alias name for the contact or unsets it if alias is null.
	 */
	void setContactAlias(Transaction txn, ContactId c, @Nullable String alias)
			throws DbException;

	/**
	 * Sets an alias name for the contact or unsets it if alias is null.
	 */
	void setContactAlias(ContactId c, @Nullable String alias)
			throws DbException;

	/**
	 * Return true if a contact with this name and public key already exists
	 */
	boolean contactExists(Transaction txn, AuthorId remoteAuthorId,
			AuthorId localAuthorId) throws DbException;

	/**
	 * Return true if a contact with this name and public key already exists
	 */
	boolean contactExists(AuthorId remoteAuthorId, AuthorId localAuthorId)
			throws DbException;

	/**
	 * Returns the {@link AuthorInfo} for the given author.
	 */
	AuthorInfo getAuthorInfo(AuthorId a) throws DbException;

	/**
	 * Returns the {@link AuthorInfo} for the given author.
	 */
	AuthorInfo getAuthorInfo(Transaction txn, AuthorId a) throws DbException;

	interface ContactHook {

		void addingContact(Transaction txn, Contact c) throws DbException;

		void removingContact(Transaction txn, Contact c) throws DbException;
	}
}

package org.briarproject.db;

import org.briarproject.api.DeviceId;
import org.briarproject.api.TransportId;
import org.briarproject.api.contact.Contact;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.db.DbException;
import org.briarproject.api.db.Metadata;
import org.briarproject.api.identity.Author;
import org.briarproject.api.identity.AuthorId;
import org.briarproject.api.identity.LocalAuthor;
import org.briarproject.api.settings.Settings;
import org.briarproject.api.sync.ClientId;
import org.briarproject.api.sync.Group;
import org.briarproject.api.sync.GroupId;
import org.briarproject.api.sync.Message;
import org.briarproject.api.sync.MessageId;
import org.briarproject.api.sync.MessageStatus;
import org.briarproject.api.sync.ValidationManager.Validity;
import org.briarproject.api.transport.TransportKeys;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * A low-level interface to the database (DatabaseComponent provides a
 * high-level interface). Most operations take a transaction argument, which is
 * obtained by calling {@link #startTransaction()}. Every transaction must be
 * terminated by calling either {@link #abortTransaction(T)} or
 * {@link #commitTransaction(T)}, even if an exception is thrown.
 */
interface Database<T> {

	/**
	 * Opens the database and returns true if the database already existed.
	 */
	boolean open() throws DbException;

	/**
	 * Prevents new transactions from starting, waits for all current
	 * transactions to finish, and closes the database.
	 */
	void close() throws DbException, IOException;

	/**
	 * Starts a new transaction and returns an object representing it.
	 */
	T startTransaction() throws DbException;

	/**
	 * Aborts the given transaction - no changes made during the transaction
	 * will be applied to the database.
	 */
	void abortTransaction(T txn);

	/**
	 * Commits the given transaction - all changes made during the transaction
	 * will be applied to the database.
	 */
	void commitTransaction(T txn) throws DbException;

	/**
	 * Stores a contact associated with the given local and remote pseudonyms,
	 * and returns an ID for the contact.
	 */
	ContactId addContact(T txn, Author remote, AuthorId local, boolean active)
			throws DbException;

	/**
	 * Stores a group.
	 */
	void addGroup(T txn, Group g) throws DbException;

	/**
	 * Stores a local pseudonym.
	 */
	void addLocalAuthor(T txn, LocalAuthor a) throws DbException;

	/**
	 * Stores a message.
	 */
	void addMessage(T txn, Message m, Validity validity, boolean shared)
			throws DbException;

	/**
	 * Records that a message has been offered by the given contact.
	 */
	void addOfferedMessage(T txn, ContactId c, MessageId m) throws DbException;

	/**
	 * Initialises the status of the given message with respect to the given
	 * contact.
	 *
	 * @param ack  whether the message needs to be acknowledged.
	 * @param seen whether the contact has seen the message.
	 */
	void addStatus(T txn, ContactId c, MessageId m, boolean ack, boolean seen)
			throws DbException;

	/**
	 * Stores a transport.
	 */
	void addTransport(T txn, TransportId t, int maxLatency)
			throws DbException;

	/**
	 * Stores transport keys for a newly added contact.
	 */
	void addTransportKeys(T txn, ContactId c, TransportKeys k)
			throws DbException;

	/**
	 * Makes a group visible to the given contact.
	 */
	void addVisibility(T txn, ContactId c, GroupId g) throws DbException;

	/**
	 * Returns true if the database contains the given contact for the given
	 * local pseudonym.
	 */
	boolean containsContact(T txn, AuthorId remote, AuthorId local)
			throws DbException;

	/**
	 * Returns true if the database contains the given contact.
	 */
	boolean containsContact(T txn, ContactId c) throws DbException;

	/**
	 * Returns true if the database contains the given group.
	 */
	boolean containsGroup(T txn, GroupId g) throws DbException;

	/**
	 * Returns true if the database contains the given local pseudonym.
	 */
	boolean containsLocalAuthor(T txn, AuthorId a) throws DbException;

	/**
	 * Returns true if the database contains the given message.
	 */
	boolean containsMessage(T txn, MessageId m) throws DbException;

	/**
	 * Returns true if the database contains the given transport.
	 */
	boolean containsTransport(T txn, TransportId t) throws DbException;

	/**
	 * Returns true if the database contains the given group and the group is
	 * visible to the given contact.
	 */
	boolean containsVisibleGroup(T txn, ContactId c, GroupId g)
			throws DbException;

	/**
	 * Returns true if the database contains the given message and the message
	 * is visible to the given contact.
	 */
	boolean containsVisibleMessage(T txn, ContactId c, MessageId m)
			throws DbException;

	/**
	 * Returns the number of messages offered by the given contact.
	 */
	int countOfferedMessages(T txn, ContactId c) throws DbException;

	/**
	 * Deletes the message with the given ID. Unlike
	 * {@link #removeMessage(Object, MessageId)}, the message ID and any other
	 * associated data are not deleted, and
	 * {@link #containsMessage(Object, MessageId)} will continue to return true.
	 * <p>
	 * Locking: write.
	 */
	void deleteMessage(T txn, MessageId m) throws DbException;

	/**
	 * Deletes any metadata associated with the given message.
	 * <p>
	 * Locking: write.
	 */
	void deleteMessageMetadata(T txn, MessageId m) throws DbException;

	/**
	 * Returns the contact with the given ID.
	 */
	Contact getContact(T txn, ContactId c) throws DbException;

	/**
	 * Returns all contacts.
	 */
	Collection<Contact> getContacts(T txn) throws DbException;

	/**
	 * Returns all contacts associated with the given local pseudonym.
	 */
	Collection<ContactId> getContacts(T txn, AuthorId a) throws DbException;

	/**
	 * Returns the unique ID for this device.
	 */
	DeviceId getDeviceId(T txn) throws DbException;

	/**
	 * Returns the amount of free storage space available to the database, in
	 * bytes. This is based on the minimum of the space available on the device
	 * where the database is stored and the database's configured size.
	 */
	long getFreeSpace() throws DbException;

	/**
	 * Returns the group with the given ID.
	 */
	Group getGroup(T txn, GroupId g) throws DbException;

	/**
	 * Returns the metadata for the given group.
	 */
	Metadata getGroupMetadata(T txn, GroupId g) throws DbException;

	/**
	 * Returns all groups belonging to the given client.
	 */
	Collection<Group> getGroups(T txn, ClientId c) throws DbException;

	/**
	 * Returns the local pseudonym with the given ID.
	 */
	LocalAuthor getLocalAuthor(T txn, AuthorId a) throws DbException;

	/**
	 * Returns all local pseudonyms.
	 */
	Collection<LocalAuthor> getLocalAuthors(T txn) throws DbException;

	/**
	 * Returns the IDs of all messages in the given group.
	 */
	Collection<MessageId> getMessageIds(T txn, GroupId g) throws DbException;

	/**
	 * Returns the metadata for all messages in the given group.
	 */
	Map<MessageId, Metadata> getMessageMetadata(T txn, GroupId g)
			throws DbException;

	/**
	 * Returns the metadata for the given message.
	 */
	Metadata getMessageMetadata(T txn, MessageId m) throws DbException;

	/**
	 * Returns the status of all messages in the given group with respect
	 * to the given contact.
	 */
	Collection<MessageStatus> getMessageStatus(T txn, ContactId c, GroupId g)
			throws DbException;

	/**
	 * Returns the status of the given message with respect to the given
	 * contact.
	 */
	MessageStatus getMessageStatus(T txn, ContactId c, MessageId m)
			throws DbException;

	/**
	 * Returns the IDs of some messages received from the given contact that
	 * need to be acknowledged, up to the given number of messages.
	 */
	Collection<MessageId> getMessagesToAck(T txn, ContactId c, int maxMessages)
			throws DbException;

	/**
	 * Returns the IDs of some messages that are eligible to be offered to the
	 * given contact, up to the given number of messages.
	 */
	Collection<MessageId> getMessagesToOffer(T txn, ContactId c,
			int maxMessages) throws DbException;

	/**
	 * Returns the IDs of some messages that are eligible to be sent to the
	 * given contact, up to the given total length.
	 */
	Collection<MessageId> getMessagesToSend(T txn, ContactId c, int maxLength)
			throws DbException;

	/**
	 * Returns the IDs of some messages that are eligible to be requested from
	 * the given contact, up to the given number of messages.
	 */
	Collection<MessageId> getMessagesToRequest(T txn, ContactId c,
			int maxMessages) throws DbException;

	/**
	 * Returns the IDs of any messages that need to be validated by the given
	 * client.
	 */
	Collection<MessageId> getMessagesToValidate(T txn, ClientId c)
			throws DbException;

	/**
	 * Returns the message with the given ID, in serialised form.
	 */
	byte[] getRawMessage(T txn, MessageId m) throws DbException;

	/**
	 * Returns the IDs of some messages that are eligible to be sent to the
	 * given contact and have been requested by the contact, up to the given
	 * total length.
	 */
	Collection<MessageId> getRequestedMessagesToSend(T txn, ContactId c,
			int maxLength) throws DbException;

	/**
	 * Returns all settings in the given namespace.
	 */
	Settings getSettings(T txn, String namespace) throws DbException;

	/**
	 * Returns all transport keys for the given transport.
	 */
	Map<ContactId, TransportKeys> getTransportKeys(T txn, TransportId t)
			throws DbException;

	/**
	 * Returns the maximum latencies in milliseconds of all transports.
	 */
	Map<TransportId, Integer> getTransportLatencies(T txn) throws DbException;

	/**
	 * Returns the IDs of all contacts to which the given group is visible.
	 */
	Collection<ContactId> getVisibility(T txn, GroupId g) throws DbException;

	/**
	 * Increments the outgoing stream counter for the given contact and
	 * transport in the given rotation period.
	 */
	void incrementStreamCounter(T txn, ContactId c, TransportId t,
			long rotationPeriod) throws DbException;

	/**
	 * Marks the given messages as not needing to be acknowledged to the
	 * given contact.
	 */
	void lowerAckFlag(T txn, ContactId c, Collection<MessageId> acked)
			throws DbException;

	/**
	 * Marks the given messages as not having been requested by the given
	 * contact.
	 */
	void lowerRequestedFlag(T txn, ContactId c, Collection<MessageId> requested)
			throws DbException;

	/*
	 * Merges the given metadata with the existing metadata for the given
	 * group.
	 */
	void mergeGroupMetadata(T txn, GroupId g, Metadata meta)
			throws DbException;

	/*
	 * Merges the given metadata with the existing metadata for the given
	 * message.
	 */
	void mergeMessageMetadata(T txn, MessageId m, Metadata meta)
			throws DbException;

	/**
	 * Merges the given settings with the existing settings in the given
	 * namespace.
	 */
	void mergeSettings(T txn, Settings s, String namespace) throws DbException;

	/**
	 * Marks a message as needing to be acknowledged to the given contact.
	 */
	void raiseAckFlag(T txn, ContactId c, MessageId m) throws DbException;

	/**
	 * Marks a message as having been requested by the given contact.
	 */
	void raiseRequestedFlag(T txn, ContactId c, MessageId m) throws DbException;

	/**
	 * Marks a message as having been seen by the given contact.
	 */
	void raiseSeenFlag(T txn, ContactId c, MessageId m) throws DbException;

	/**
	 * Removes a contact from the database.
	 */
	void removeContact(T txn, ContactId c) throws DbException;

	/**
	 * Removes a group (and all associated state) from the database.
	 */
	void removeGroup(T txn, GroupId g) throws DbException;

	/**
	 * Removes a local pseudonym (and all associated state) from the database.
	 */
	void removeLocalAuthor(T txn, AuthorId a) throws DbException;

	/**
	 * Removes a message (and all associated state) from the database.
	 */
	void removeMessage(T txn, MessageId m) throws DbException;

	/**
	 * Removes an offered message that was offered by the given contact, or
	 * returns false if there is no such message.
	 */
	boolean removeOfferedMessage(T txn, ContactId c, MessageId m)
			throws DbException;

	/**
	 * Removes the given offered messages that were offered by the given
	 * contact.
	 */
	void removeOfferedMessages(T txn, ContactId c,
			Collection<MessageId> requested) throws DbException;

	/**
	 * Removes the status of the given message with respect to the given
	 * contact.
	 */
	void removeStatus(T txn, ContactId c, MessageId m) throws DbException;

	/**
	 * Removes a transport (and all associated state) from the database.
	 */
	void removeTransport(T txn, TransportId t) throws DbException;

	/**
	 * Makes a group invisible to the given contact.
	 */
	void removeVisibility(T txn, ContactId c, GroupId g) throws DbException;

	/**
	 * Resets the transmission count and expiry time of the given message with
	 * respect to the given contact.
	 */
	void resetExpiryTime(T txn, ContactId c, MessageId m) throws DbException;

	/**
	 * Marks the given contact as active or inactive.
	 */
	void setContactActive(T txn, ContactId c, boolean active)
		throws DbException;

	/**
	 * Marks the given message as shared or unshared.
	 */
	void setMessageShared(T txn, MessageId m, boolean shared)
			throws DbException;

	/**
	 * Marks the given message as valid or invalid.
	 */
	void setMessageValid(T txn, MessageId m, boolean valid) throws DbException;

	/**
	 * Sets the reordering window for the given contact and transport in the
	 * given rotation period.
	 */
	void setReorderingWindow(T txn, ContactId c, TransportId t,
			long rotationPeriod, long base, byte[] bitmap) throws DbException;

	/**
	 * Updates the transmission count and expiry time of the given message
	 * with respect to the given contact, using the latency of the transport
	 * over which it was sent.
	 */
	void updateExpiryTime(T txn, ContactId c, MessageId m, int maxLatency)
			throws DbException;

	/**
	 * Stores the given transport keys, deleting any keys they have replaced.
	 */
	void updateTransportKeys(T txn, Map<ContactId, TransportKeys> keys)
			throws DbException;
}

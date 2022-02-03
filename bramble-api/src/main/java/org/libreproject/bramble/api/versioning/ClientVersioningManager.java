package org.libreproject.bramble.api.versioning;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.crypto.SecretKey;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.ClientId;
import org.libreproject.bramble.api.sync.Group.Visibility;

@NotNullByDefault
public interface ClientVersioningManager {

	/**
	 * The unique ID of the versioning client.
	 */
	ClientId CLIENT_ID = new ClientId("org.briarproject.bramble.versioning");

	/**
	 * The current major version of the versioning client.
	 */
	int MAJOR_VERSION = 0;

	/**
	 * Registers a client that will be advertised to contacts. The hook will
	 * be called when the visibility of the client changes. This method should
	 * be called before {@link LifecycleManager#startServices(SecretKey)}.
	 */
	void registerClient(ClientId clientId, int majorVersion, int minorVersion,
			ClientVersioningHook hook);

	/**
	 * Returns the visibility of the given client with respect to the given
	 * contact.
	 */
	Visibility getClientVisibility(Transaction txn, ContactId contactId,
			ClientId clientId, int majorVersion) throws DbException;

	/**
	 * Returns the minor version of the given client that is supported by the
	 * given contact, or -1 if the contact does not support the client.
	 */
	int getClientMinorVersion(Transaction txn, ContactId contactId,
			ClientId clientId, int majorVersion) throws DbException;

	interface ClientVersioningHook {
		/**
		 * Called when the visibility of a client with respect to a contact is
		 * changing.
		 *
		 * @param txn A read-write transaction
		 * @param c The contact affected by the visibility change
		 * @param v The new visibility of the client
		 */
		void onClientVisibilityChanging(Transaction txn, Contact c,
				Visibility v) throws DbException;
	}
}

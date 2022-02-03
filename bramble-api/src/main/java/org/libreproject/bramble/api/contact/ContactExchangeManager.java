package org.libreproject.bramble.api.contact;

import org.libreproject.bramble.api.crypto.SecretKey;
import org.libreproject.bramble.api.db.ContactExistsException;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.duplex.DuplexTransportConnection;

import java.io.IOException;

@NotNullByDefault
public interface ContactExchangeManager {

	/**
	 * Exchanges contact information with a remote peer and adds the peer
	 * as a contact.
	 *
	 * @param alice Whether the local peer takes the role of Alice
	 * @return The newly added contact
	 * @throws ContactExistsException If the contact already exists
	 */
	Contact exchangeContacts(DuplexTransportConnection conn,
			SecretKey masterKey, boolean alice, boolean verified)
			throws IOException, DbException;

	/**
	 * Exchanges contact information with a remote peer and adds the peer
	 * as a contact, replacing the given pending contact.
	 *
	 * @param alice Whether the local peer takes the role of Alice
	 * @return The newly added contact
	 * @throws ContactExistsException If the contact already exists
	 */
	Contact exchangeContacts(PendingContactId p, DuplexTransportConnection conn,
			SecretKey masterKey, boolean alice, boolean verified)
			throws IOException, DbException;
}

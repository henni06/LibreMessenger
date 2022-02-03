package org.libreproject.bramble.transport;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.contact.PendingContactId;
import org.libreproject.bramble.api.crypto.SecretKey;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.transport.KeySetId;
import org.libreproject.bramble.api.transport.StreamContext;

import javax.annotation.Nullable;

@NotNullByDefault
interface TransportKeyManager {

	void start(Transaction txn) throws DbException;

	KeySetId addRotationKeys(Transaction txn, ContactId c,
			SecretKey rootKey, long timestamp, boolean alice, boolean active)
			throws DbException;

	KeySetId addHandshakeKeys(Transaction txn, ContactId c,
			SecretKey rootKey, boolean alice) throws DbException;

	KeySetId addHandshakeKeys(Transaction txn, PendingContactId p,
			SecretKey rootKey, boolean alice) throws DbException;

	void activateKeys(Transaction txn, KeySetId k) throws DbException;

	void removeContact(ContactId c);

	void removePendingContact(PendingContactId p);

	boolean canSendOutgoingStreams(ContactId c);

	boolean canSendOutgoingStreams(PendingContactId p);

	@Nullable
	StreamContext getStreamContext(Transaction txn, ContactId c)
			throws DbException;

	@Nullable
	StreamContext getStreamContext(Transaction txn, PendingContactId p)
			throws DbException;

	@Nullable
	StreamContext getStreamContext(Transaction txn, byte[] tag)
			throws DbException;

}

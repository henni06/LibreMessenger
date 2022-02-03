package org.libreproject.bramble.api.mailbox;

import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;

@NotNullByDefault
public interface MailboxSettingsManager {

	@Nullable
	MailboxProperties getOwnMailboxProperties(Transaction txn)
			throws DbException;

	void setOwnMailboxProperties(Transaction txn, MailboxProperties p)
			throws DbException;

	MailboxStatus getOwnMailboxStatus(Transaction txn) throws DbException;

	void recordSuccessfulConnection(Transaction txn, long now)
			throws DbException;

	void recordFailedConnectionAttempt(Transaction txn, long now)
			throws DbException;
}

package org.libreproject.bramble.api.db;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface DbRunnable<E extends Exception> {

	void run(Transaction txn) throws DbException, E;
}

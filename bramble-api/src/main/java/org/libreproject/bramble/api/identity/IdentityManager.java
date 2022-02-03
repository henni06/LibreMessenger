package org.libreproject.bramble.api.identity;

import org.libreproject.bramble.api.crypto.CryptoExecutor;
import org.libreproject.bramble.api.crypto.KeyPair;
import org.libreproject.bramble.api.crypto.SecretKey;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface IdentityManager {

	/**
	 * Creates an identity with the given name. The identity includes a
	 * handshake key pair.
	 */
	@CryptoExecutor
	Identity createIdentity(String name);

	/**
	 * Registers the given identity with the manager. This method should be
	 * called before {@link LifecycleManager#startServices(SecretKey)}. The
	 * identity is stored when {@link LifecycleManager#startServices(SecretKey)}
	 * is called. The identity must include a handshake key pair.
	 */
	void registerIdentity(Identity i);

	/**
	 * Returns the cached local identity or loads it from the database.
	 */
	LocalAuthor getLocalAuthor() throws DbException;

	/**
	 * Returns the cached local identity or loads it from the database.
	 * <p/>
	 * Read-only.
	 */
	LocalAuthor getLocalAuthor(Transaction txn) throws DbException;

	/**
	 * Returns the cached handshake keys or loads them from the database.
	 * <p/>
	 * Read-only.
	 */
	KeyPair getHandshakeKeys(Transaction txn) throws DbException;
}

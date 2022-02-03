package org.libreproject.libre.api.client;

import org.libreproject.bramble.api.db.DbException;

/**
 * Thrown when a database operation is attempted as part of a protocol session
 * and the operation is not applicable to the current protocol state. This
 * exception may occur due to concurrent updates and does not indicate a
 * database error.
 */
public class ProtocolStateException extends DbException {
}

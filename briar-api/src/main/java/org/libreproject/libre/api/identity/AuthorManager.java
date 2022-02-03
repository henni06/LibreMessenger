package org.libreproject.libre.api.identity;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.identity.AuthorId;
import org.libreproject.bramble.api.identity.LocalAuthor;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface AuthorManager {

	/**
	 * Returns the {@link AuthorInfo} for the given author.
	 */
	AuthorInfo getAuthorInfo(AuthorId a) throws DbException;

	/**
	 * Returns the {@link AuthorInfo} for the given author.
	 */
	AuthorInfo getAuthorInfo(Transaction txn, AuthorId a) throws DbException;

	/**
	 * Returns the {@link AuthorInfo} for the given contact.
	 */
	AuthorInfo getAuthorInfo(Contact c) throws DbException;

	/**
	 * Returns the {@link AuthorInfo} for the given contact.
	 */
	AuthorInfo getAuthorInfo(Transaction txn, Contact c)
			throws DbException;

	/**
	 * Returns the {@link AuthorInfo} for the {@link LocalAuthor}.
	 */
	AuthorInfo getMyAuthorInfo() throws DbException;

	/**
	 * Returns the {@link AuthorInfo} for the {@link LocalAuthor}.
	 */
	AuthorInfo getMyAuthorInfo(Transaction txn) throws DbException;
}

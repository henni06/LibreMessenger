package org.libreproject.libre.identity;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.db.DatabaseComponent;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.identity.AuthorId;
import org.libreproject.bramble.api.identity.IdentityManager;
import org.libreproject.bramble.api.identity.LocalAuthor;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.test.BrambleMockTestCase;
import org.libreproject.bramble.test.DbExpectations;
import org.libreproject.libre.api.attachment.AttachmentHeader;
import org.libreproject.libre.api.avatar.AvatarManager;
import org.libreproject.libre.api.identity.AuthorInfo;
import org.jmock.Expectations;
import org.junit.Test;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.libreproject.bramble.test.TestUtils.getAuthor;
import static org.libreproject.bramble.test.TestUtils.getContact;
import static org.libreproject.bramble.test.TestUtils.getLocalAuthor;
import static org.libreproject.bramble.test.TestUtils.getRandomId;
import static org.libreproject.bramble.util.StringUtils.getRandomString;
import static org.libreproject.libre.api.attachment.MediaConstants.MAX_CONTENT_TYPE_BYTES;
import static org.libreproject.libre.api.identity.AuthorInfo.Status.OURSELVES;
import static org.libreproject.libre.api.identity.AuthorInfo.Status.UNKNOWN;
import static org.libreproject.libre.api.identity.AuthorInfo.Status.UNVERIFIED;
import static org.libreproject.libre.api.identity.AuthorInfo.Status.VERIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AuthorManagerImplTest extends BrambleMockTestCase {

	private final DatabaseComponent db = context.mock(DatabaseComponent.class);
	private final IdentityManager identityManager =
			context.mock(IdentityManager.class);
	private final AvatarManager avatarManager =
			context.mock(AvatarManager.class);

	private final Author remote = getAuthor();
	private final LocalAuthor localAuthor = getLocalAuthor();
	private final AuthorId local = localAuthor.getId();
	private final boolean verified = false;
	private final Contact contact = getContact(remote, local, verified);
	private final String contentType = getRandomString(MAX_CONTENT_TYPE_BYTES);
	private final AttachmentHeader avatarHeader =
			new AttachmentHeader(new GroupId(getRandomId()),
					new MessageId(getRandomId()), contentType);

	private final AuthorManagerImpl authorManager =
			new AuthorManagerImpl(db, identityManager, avatarManager);

	@Test
	public void testGetAuthorInfoUnverified() throws Exception {
		Transaction txn = new Transaction(null, true);

		checkAuthorInfoContext(txn, remote.getId(), singletonList(contact));
		context.checking(new DbExpectations() {{
			oneOf(avatarManager).getAvatarHeader(txn, contact);
			will(returnValue(avatarHeader));
		}});

		AuthorInfo authorInfo =
				authorManager.getAuthorInfo(txn, remote.getId());
		assertEquals(UNVERIFIED, authorInfo.getStatus());
		assertEquals(contact.getAlias(), authorInfo.getAlias());
		assertEquals(avatarHeader, authorInfo.getAvatarHeader());
	}

	@Test
	public void testGetAuthorInfoUnknown() throws DbException {
		Transaction txn = new Transaction(null, true);

		checkAuthorInfoContext(txn, remote.getId(), emptyList());

		AuthorInfo authorInfo =
				authorManager.getAuthorInfo(txn, remote.getId());
		assertEquals(UNKNOWN, authorInfo.getStatus());
		assertNull(authorInfo.getAlias());
		assertNull(authorInfo.getAvatarHeader());
	}

	@Test
	public void testGetAuthorInfoVerified() throws DbException {
		Transaction txn = new Transaction(null, true);

		Contact verified = getContact(remote, local, true);
		checkAuthorInfoContext(txn, remote.getId(), singletonList(verified));
		context.checking(new DbExpectations() {{
			oneOf(avatarManager).getAvatarHeader(txn, verified);
			will(returnValue(avatarHeader));
		}});

		AuthorInfo authorInfo =
				authorManager.getAuthorInfo(txn, remote.getId());
		assertEquals(VERIFIED, authorInfo.getStatus());
		assertEquals(verified.getAlias(), authorInfo.getAlias());
		assertEquals(avatarHeader, authorInfo.getAvatarHeader());
	}

	@Test
	public void testGetAuthorInfoOurselves() throws DbException {
		Transaction txn = new Transaction(null, true);

		context.checking(new Expectations() {{
			oneOf(identityManager).getLocalAuthor(txn);
			will(returnValue(localAuthor));
			never(db).getContactsByAuthorId(txn, remote.getId());
			oneOf(avatarManager).getMyAvatarHeader(txn);
			will(returnValue(avatarHeader));
		}});

		AuthorInfo authorInfo =
				authorManager.getAuthorInfo(txn, localAuthor.getId());
		assertEquals(OURSELVES, authorInfo.getStatus());
		assertNull(authorInfo.getAlias());
		assertEquals(avatarHeader, authorInfo.getAvatarHeader());
	}

	@Test
	public void testGetMyAuthorInfo() throws DbException {
		Transaction txn = new Transaction(null, true);

		context.checking(new Expectations() {{
			oneOf(avatarManager).getMyAvatarHeader(txn);
			will(returnValue(avatarHeader));
		}});

		AuthorInfo authorInfo =
				authorManager.getMyAuthorInfo(txn);
		assertEquals(OURSELVES, authorInfo.getStatus());
		assertNull(authorInfo.getAlias());
		assertEquals(avatarHeader, authorInfo.getAvatarHeader());
	}

	private void checkAuthorInfoContext(Transaction txn, AuthorId authorId,
			Collection<Contact> contacts) throws DbException {
		context.checking(new Expectations() {{
			oneOf(identityManager).getLocalAuthor(txn);
			will(returnValue(localAuthor));
			oneOf(db).getContactsByAuthorId(txn, authorId);
			will(returnValue(contacts));
		}});
	}

}

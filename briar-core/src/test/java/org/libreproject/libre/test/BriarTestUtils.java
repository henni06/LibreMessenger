package org.libreproject.libre.test;

import org.libreproject.bramble.api.crypto.CryptoComponent;
import org.libreproject.bramble.api.crypto.KeyPair;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.identity.AuthorFactory;
import org.libreproject.bramble.api.identity.LocalAuthor;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.util.Base32;
import org.libreproject.libre.api.client.MessageTracker;
import org.libreproject.libre.api.client.MessageTracker.GroupCount;

import static java.lang.System.arraycopy;
import static org.libreproject.bramble.api.contact.HandshakeLinkConstants.FORMAT_VERSION;
import static org.libreproject.bramble.api.contact.HandshakeLinkConstants.RAW_LINK_BYTES;
import static org.libreproject.bramble.api.identity.AuthorConstants.MAX_AUTHOR_NAME_LENGTH;
import static org.libreproject.bramble.util.StringUtils.getRandomString;
import static org.junit.Assert.assertEquals;

public class BriarTestUtils {

	public static void assertGroupCount(MessageTracker tracker, GroupId g,
			long msgCount, long unreadCount, long latestMsgTime)
			throws DbException {
		GroupCount groupCount = tracker.getGroupCount(g);
		assertEquals(msgCount, groupCount.getMsgCount());
		assertEquals(unreadCount, groupCount.getUnreadCount());
		assertEquals(latestMsgTime, groupCount.getLatestMsgTime());
	}

	public static void assertGroupCount(MessageTracker tracker, GroupId g,
			long msgCount, long unreadCount) throws DbException {
		GroupCount c1 = tracker.getGroupCount(g);
		assertEquals(msgCount, c1.getMsgCount());
		assertEquals(unreadCount, c1.getUnreadCount());
	}

	public static Author getRealAuthor(AuthorFactory authorFactory) {
		String name = getRandomString(MAX_AUTHOR_NAME_LENGTH);
		return authorFactory.createLocalAuthor(name);
	}

	public static LocalAuthor getRealLocalAuthor(AuthorFactory authorFactory) {
		String name = getRandomString(MAX_AUTHOR_NAME_LENGTH);
		return authorFactory.createLocalAuthor(name);
	}

	public static String getRealHandshakeLink(CryptoComponent cryptoComponent) {
		KeyPair keyPair = cryptoComponent.generateAgreementKeyPair();
		byte[] linkBytes = new byte[RAW_LINK_BYTES];
		byte[] publicKey = keyPair.getPublic().getEncoded();
		linkBytes[0] = FORMAT_VERSION;
		arraycopy(publicKey, 0, linkBytes, 1, RAW_LINK_BYTES - 1);
		return ("briar://" + Base32.encode(linkBytes)).toLowerCase();
	}

}

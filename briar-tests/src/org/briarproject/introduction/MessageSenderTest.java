package org.briarproject.introduction;

import org.briarproject.BriarTestCase;
import org.briarproject.TestUtils;
import org.briarproject.api.FormatException;
import org.briarproject.api.clients.ClientHelper;
import org.briarproject.api.clients.MessageQueueManager;
import org.briarproject.api.clients.SessionId;
import org.briarproject.api.data.BdfDictionary;
import org.briarproject.api.data.BdfEntry;
import org.briarproject.api.data.BdfList;
import org.briarproject.api.data.MetadataEncoder;
import org.briarproject.api.db.DatabaseComponent;
import org.briarproject.api.db.DbException;
import org.briarproject.api.db.Metadata;
import org.briarproject.api.db.Transaction;
import org.briarproject.api.sync.ClientId;
import org.briarproject.api.sync.Group;
import org.briarproject.api.sync.GroupId;
import org.briarproject.api.system.Clock;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static org.briarproject.api.identity.AuthorConstants.MAX_SIGNATURE_LENGTH;
import static org.briarproject.api.introduction.IntroductionConstants.GROUP_ID;
import static org.briarproject.api.introduction.IntroductionConstants.MAC;
import static org.briarproject.api.introduction.IntroductionConstants.SESSION_ID;
import static org.briarproject.api.introduction.IntroductionConstants.SIGNATURE;
import static org.briarproject.api.introduction.IntroductionConstants.TYPE;
import static org.briarproject.api.introduction.IntroductionConstants.TYPE_ACK;

public class MessageSenderTest extends BriarTestCase {

	private final Mockery context;
	private final MessageSender messageSender;
	private final DatabaseComponent db;
	private final ClientHelper clientHelper;
	private final MetadataEncoder metadataEncoder;
	private final MessageQueueManager messageQueueManager;
	private final Clock clock;

	public MessageSenderTest() {
		context = new Mockery();
		db = context.mock(DatabaseComponent.class);
		clientHelper = context.mock(ClientHelper.class);
		metadataEncoder =
				context.mock(MetadataEncoder.class);
		messageQueueManager =
				context.mock(MessageQueueManager.class);
		clock = context.mock(Clock.class);

		messageSender =
				new MessageSender(db, clientHelper, clock, metadataEncoder,
						messageQueueManager);
	}

	@Test
	public void testSendMessage() throws DbException, FormatException {
		final Transaction txn = new Transaction(null, false);
		final Group privateGroup =
				new Group(new GroupId(TestUtils.getRandomId()),
						new ClientId(TestUtils.getRandomId()), new byte[0]);
		final SessionId sessionId = new SessionId(TestUtils.getRandomId());
		byte[] mac = TestUtils.getRandomBytes(42);
		byte[] sig = TestUtils.getRandomBytes(MAX_SIGNATURE_LENGTH);
		final long time = 42L;
		final BdfDictionary msg = BdfDictionary.of(
				new BdfEntry(TYPE, TYPE_ACK),
				new BdfEntry(GROUP_ID, privateGroup.getId()),
				new BdfEntry(SESSION_ID, sessionId),
				new BdfEntry(MAC, mac),
				new BdfEntry(SIGNATURE, sig)
		);
		final BdfList bodyList =
				BdfList.of(TYPE_ACK, sessionId.getBytes(), mac, sig);
		final byte[] body = TestUtils.getRandomBytes(8);
		final Metadata metadata = new Metadata();

		context.checking(new Expectations() {{
			oneOf(clientHelper).toByteArray(bodyList);
			will(returnValue(body));
			oneOf(db).getGroup(txn, privateGroup.getId());
			will(returnValue(privateGroup));
			oneOf(metadataEncoder).encode(msg);
			will(returnValue(metadata));
			oneOf(clock).currentTimeMillis();
			will(returnValue(time));
			oneOf(messageQueueManager)
					.sendMessage(txn, privateGroup, time, body, metadata);
		}});

		messageSender.sendMessage(txn, msg);

		context.assertIsSatisfied();
		assertFalse(txn.isComplete());
	}

}

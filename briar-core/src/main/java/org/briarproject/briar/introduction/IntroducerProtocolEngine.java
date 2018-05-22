package org.briarproject.briar.introduction;

import org.briarproject.bramble.api.client.ClientHelper;
import org.briarproject.bramble.api.client.ContactGroupFactory;
import org.briarproject.bramble.api.contact.ContactManager;
import org.briarproject.bramble.api.db.DatabaseComponent;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.Transaction;
import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.identity.IdentityManager;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.Message;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.bramble.api.system.Clock;
import org.briarproject.briar.api.client.MessageTracker;
import org.briarproject.briar.api.client.ProtocolStateException;
import org.briarproject.briar.api.introduction.event.IntroductionAbortedEvent;
import org.briarproject.briar.introduction.IntroducerSession.Introducee;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.briarproject.briar.introduction.IntroducerState.AWAIT_ACTIVATES;
import static org.briarproject.briar.introduction.IntroducerState.AWAIT_ACTIVATE_A;
import static org.briarproject.briar.introduction.IntroducerState.AWAIT_ACTIVATE_B;
import static org.briarproject.briar.introduction.IntroducerState.AWAIT_AUTHS;
import static org.briarproject.briar.introduction.IntroducerState.AWAIT_AUTH_A;
import static org.briarproject.briar.introduction.IntroducerState.AWAIT_AUTH_B;
import static org.briarproject.briar.introduction.IntroducerState.AWAIT_RESPONSES;
import static org.briarproject.briar.introduction.IntroducerState.AWAIT_RESPONSE_A;
import static org.briarproject.briar.introduction.IntroducerState.AWAIT_RESPONSE_B;
import static org.briarproject.briar.introduction.IntroducerState.A_DECLINED;
import static org.briarproject.briar.introduction.IntroducerState.B_DECLINED;
import static org.briarproject.briar.introduction.IntroducerState.START;

@Immutable
@NotNullByDefault
class IntroducerProtocolEngine
		extends AbstractProtocolEngine<IntroducerSession> {

	@Inject
	IntroducerProtocolEngine(
			DatabaseComponent db,
			ClientHelper clientHelper,
			ContactManager contactManager,
			ContactGroupFactory contactGroupFactory,
			MessageTracker messageTracker,
			IdentityManager identityManager,
			MessageParser messageParser,
			MessageEncoder messageEncoder,
			Clock clock) {
		super(db, clientHelper, contactManager, contactGroupFactory,
				messageTracker, identityManager, messageParser, messageEncoder,
				clock);
	}

	@Override
	public IntroducerSession onRequestAction(Transaction txn,
			IntroducerSession s, @Nullable String message, long timestamp)
			throws DbException {
		switch (s.getState()) {
			case START:
				return onLocalRequest(txn, s, message, timestamp);
			case AWAIT_RESPONSES:
			case AWAIT_RESPONSE_A:
			case AWAIT_RESPONSE_B:
			case A_DECLINED:
			case B_DECLINED:
			case AWAIT_AUTHS:
			case AWAIT_AUTH_A:
			case AWAIT_AUTH_B:
			case AWAIT_ACTIVATES:
			case AWAIT_ACTIVATE_A:
			case AWAIT_ACTIVATE_B:
				throw new ProtocolStateException(); // Invalid in these states
			default:
				throw new AssertionError();
		}
	}

	@Override
	public IntroducerSession onAcceptAction(Transaction txn,
			IntroducerSession s, long timestamp) {
		throw new UnsupportedOperationException(); // Invalid in this role
	}

	@Override
	public IntroducerSession onDeclineAction(Transaction txn,
			IntroducerSession s, long timestamp) {
		throw new UnsupportedOperationException(); // Invalid in this role
	}

	IntroducerSession onIntroduceeRemoved(Transaction txn,
			Introducee remainingIntroducee, IntroducerSession session)
			throws DbException {
		// abort session
		IntroducerSession s = abort(txn, session);
		// reset information for introducee that was removed
		Introducee introduceeA, introduceeB;
		if (remainingIntroducee.author.equals(s.getIntroduceeA().author)) {
			introduceeA = s.getIntroduceeA();
			introduceeB =
					new Introducee(s.getSessionId(), s.getIntroduceeB().groupId,
							s.getIntroduceeB().author);
		} else if (remainingIntroducee.author
				.equals(s.getIntroduceeB().author)) {
			introduceeA =
					new Introducee(s.getSessionId(), s.getIntroduceeA().groupId,
							s.getIntroduceeA().author);
			introduceeB = s.getIntroduceeB();
		} else throw new DbException();
		return new IntroducerSession(s.getSessionId(), s.getState(),
				s.getRequestTimestamp(), introduceeA, introduceeB);
	}

	@Override
	public IntroducerSession onRequestMessage(Transaction txn,
			IntroducerSession s, RequestMessage m) throws DbException {
		return abort(txn, s); // Invalid in this role
	}

	@Override
	public IntroducerSession onAcceptMessage(Transaction txn,
			IntroducerSession s, AcceptMessage m) throws DbException {
		switch (s.getState()) {
			case AWAIT_RESPONSES:
			case AWAIT_RESPONSE_A:
			case AWAIT_RESPONSE_B:
				return onRemoteAccept(txn, s, m);
			case A_DECLINED:
			case B_DECLINED:
				return onRemoteAcceptWhenDeclined(txn, s, m);
			case START:
			case AWAIT_AUTHS:
			case AWAIT_AUTH_A:
			case AWAIT_AUTH_B:
			case AWAIT_ACTIVATES:
			case AWAIT_ACTIVATE_A:
			case AWAIT_ACTIVATE_B:
				return abort(txn, s); // Invalid in these states
			default:
				throw new AssertionError();
		}
	}

	@Override
	public IntroducerSession onDeclineMessage(Transaction txn,
			IntroducerSession s, DeclineMessage m) throws DbException {
		switch (s.getState()) {
			case AWAIT_RESPONSES:
			case AWAIT_RESPONSE_A:
			case AWAIT_RESPONSE_B:
				return onRemoteDecline(txn, s, m);
			case A_DECLINED:
			case B_DECLINED:
				return onRemoteDeclineWhenDeclined(txn, s, m);
			case START:
			case AWAIT_AUTHS:
			case AWAIT_AUTH_A:
			case AWAIT_AUTH_B:
			case AWAIT_ACTIVATES:
			case AWAIT_ACTIVATE_A:
			case AWAIT_ACTIVATE_B:
				return abort(txn, s); // Invalid in these states
			default:
				throw new AssertionError();
		}
	}

	@Override
	public IntroducerSession onAuthMessage(Transaction txn, IntroducerSession s,
			AuthMessage m) throws DbException {
		switch (s.getState()) {
			case AWAIT_AUTHS:
			case AWAIT_AUTH_A:
			case AWAIT_AUTH_B:
				return onRemoteAuth(txn, s, m);
			case START:
			case AWAIT_RESPONSES:
			case AWAIT_RESPONSE_A:
			case AWAIT_RESPONSE_B:
			case A_DECLINED:
			case B_DECLINED:
			case AWAIT_ACTIVATES:
			case AWAIT_ACTIVATE_A:
			case AWAIT_ACTIVATE_B:
				return abort(txn, s); // Invalid in these states
			default:
				throw new AssertionError();
		}
	}

	@Override
	public IntroducerSession onActivateMessage(Transaction txn,
			IntroducerSession s, ActivateMessage m) throws DbException {
		switch (s.getState()) {
			case AWAIT_ACTIVATES:
			case AWAIT_ACTIVATE_A:
			case AWAIT_ACTIVATE_B:
				return onRemoteActivate(txn, s, m);
			case START:
			case AWAIT_RESPONSES:
			case AWAIT_RESPONSE_A:
			case AWAIT_RESPONSE_B:
			case A_DECLINED:
			case B_DECLINED:
			case AWAIT_AUTHS:
			case AWAIT_AUTH_A:
			case AWAIT_AUTH_B:
				return abort(txn, s); // Invalid in these states
			default:
				throw new AssertionError();
		}
	}

	@Override
	public IntroducerSession onAbortMessage(Transaction txn,
			IntroducerSession s, AbortMessage m) throws DbException {
		return onRemoteAbort(txn, s, m);
	}

	private IntroducerSession onLocalRequest(Transaction txn,
			IntroducerSession s, @Nullable String message, long timestamp)
			throws DbException {
		// Send REQUEST messages
		long maxIntroduceeTimestamp =
				Math.max(getLocalTimestamp(s, s.getIntroduceeA()),
						getLocalTimestamp(s, s.getIntroduceeB()));
		long localTimestamp = Math.max(timestamp, maxIntroduceeTimestamp);
		Message sentA = sendRequestMessage(txn, s.getIntroduceeA(),
				localTimestamp, s.getIntroduceeB().author, message
		);
		Message sentB = sendRequestMessage(txn, s.getIntroduceeB(),
				localTimestamp, s.getIntroduceeA().author, message
		);
		// Track the messages
		messageTracker.trackOutgoingMessage(txn, sentA);
		messageTracker.trackOutgoingMessage(txn, sentB);
		// Move to the AWAIT_RESPONSES state
		Introducee introduceeA = new Introducee(s.getIntroduceeA(), sentA);
		Introducee introduceeB = new Introducee(s.getIntroduceeB(), sentB);
		return new IntroducerSession(s.getSessionId(), AWAIT_RESPONSES,
				localTimestamp, introduceeA, introduceeB);
	}

	private IntroducerSession onRemoteAccept(Transaction txn,
			IntroducerSession s, AcceptMessage m) throws DbException {
		// The timestamp must be higher than the last request message
		if (m.getTimestamp() <= s.getRequestTimestamp())
			return abort(txn, s);
		// The dependency, if any, must be the last remote message
		if (isInvalidDependency(s, m.getGroupId(), m.getPreviousMessageId()))
			return abort(txn, s);
		// The message must be expected in the current state
		boolean senderIsAlice = senderIsAlice(s, m);
		if (s.getState() != AWAIT_RESPONSES) {
			if (senderIsAlice && s.getState() != AWAIT_RESPONSE_A)
				return abort(txn, s);
			else if (!senderIsAlice && s.getState() != AWAIT_RESPONSE_B)
				return abort(txn, s);
		}

		// Mark the response visible in the UI
		markMessageVisibleInUi(txn, m.getMessageId());
		// Track the incoming message
		messageTracker
				.trackMessage(txn, m.getGroupId(), m.getTimestamp(), false);

		// Forward ACCEPT message
		Introducee i = getOtherIntroducee(s, m.getGroupId());
		long timestamp = getLocalTimestamp(s, i);
		Message sent =
				sendAcceptMessage(txn, i, timestamp, m.getEphemeralPublicKey(),
						m.getAcceptTimestamp(), m.getTransportProperties(),
						false);

		// Create the next state
		IntroducerState state = AWAIT_AUTHS;
		Introducee introduceeA, introduceeB;
		Author sender, other;
		if (senderIsAlice) {
			if (s.getState() == AWAIT_RESPONSES) state = AWAIT_RESPONSE_B;
			introduceeA = new Introducee(s.getIntroduceeA(), m.getMessageId());
			introduceeB = new Introducee(s.getIntroduceeB(), sent);
			sender = introduceeA.author;
			other = introduceeB.author;
		} else {
			if (s.getState() == AWAIT_RESPONSES) state = AWAIT_RESPONSE_A;
			introduceeA = new Introducee(s.getIntroduceeA(), sent);
			introduceeB = new Introducee(s.getIntroduceeB(), m.getMessageId());
			sender = introduceeB.author;
			other = introduceeA.author;
		}

		// Broadcast IntroductionResponseReceivedEvent
		broadcastIntroductionResponseReceivedEvent(txn, s, sender.getId(),
				other, m);

		// Move to the next state
		return new IntroducerSession(s.getSessionId(), state,
				s.getRequestTimestamp(), introduceeA, introduceeB);
	}

	private boolean senderIsAlice(IntroducerSession s,
			AbstractIntroductionMessage m) {
		return m.getGroupId().equals(s.getIntroduceeA().groupId);
	}

	private IntroducerSession onRemoteAcceptWhenDeclined(Transaction txn,
			IntroducerSession s, AcceptMessage m) throws DbException {
		// The timestamp must be higher than the last request message
		if (m.getTimestamp() <= s.getRequestTimestamp())
			return abort(txn, s);
		// The dependency, if any, must be the last remote message
		if (isInvalidDependency(s, m.getGroupId(), m.getPreviousMessageId()))
			return abort(txn, s);
		// The message must be expected in the current state
		boolean senderIsAlice = senderIsAlice(s, m);
		if (senderIsAlice && s.getState() != B_DECLINED)
			return abort(txn, s);
		else if (!senderIsAlice && s.getState() != A_DECLINED)
			return abort(txn, s);

		// Mark the response visible in the UI
		markMessageVisibleInUi(txn, m.getMessageId());
		// Track the incoming message
		messageTracker
				.trackMessage(txn, m.getGroupId(), m.getTimestamp(), false);

		// Forward ACCEPT message
		Introducee i = getOtherIntroducee(s, m.getGroupId());
		Message sent = sendAcceptMessage(txn, i, getLocalTimestamp(s, i),
				m.getEphemeralPublicKey(), m.getAcceptTimestamp(),
				m.getTransportProperties(), false);

		Introducee introduceeA, introduceeB;
		Author sender, other;
		if (senderIsAlice) {
			introduceeA = new Introducee(s.getIntroduceeA(), m.getMessageId());
			introduceeB = new Introducee(s.getIntroduceeB(), sent);
			sender = introduceeA.author;
			other = introduceeB.author;
		} else {
			introduceeA = new Introducee(s.getIntroduceeA(), sent);
			introduceeB = new Introducee(s.getIntroduceeB(), m.getMessageId());
			sender = introduceeB.author;
			other = introduceeA.author;
		}

		// Broadcast IntroductionResponseReceivedEvent
		broadcastIntroductionResponseReceivedEvent(txn, s, sender.getId(),
				other, m);

		return new IntroducerSession(s.getSessionId(), START,
				s.getRequestTimestamp(), introduceeA, introduceeB);
	}

	private IntroducerSession onRemoteDecline(Transaction txn,
			IntroducerSession s, DeclineMessage m) throws DbException {
		// The timestamp must be higher than the last request message
		if (m.getTimestamp() <= s.getRequestTimestamp())
			return abort(txn, s);
		// The dependency, if any, must be the last remote message
		if (isInvalidDependency(s, m.getGroupId(), m.getPreviousMessageId()))
			return abort(txn, s);
		// The message must be expected in the current state
		boolean senderIsAlice = senderIsAlice(s, m);
		if (s.getState() != AWAIT_RESPONSES) {
			if (senderIsAlice && s.getState() != AWAIT_RESPONSE_A)
				return abort(txn, s);
			else if (!senderIsAlice && s.getState() != AWAIT_RESPONSE_B)
				return abort(txn, s);
		}

		// Mark the response visible in the UI
		markMessageVisibleInUi(txn, m.getMessageId());
		// Track the incoming message
		messageTracker
				.trackMessage(txn, m.getGroupId(), m.getTimestamp(), false);

		// Forward DECLINE message
		Introducee i = getOtherIntroducee(s, m.getGroupId());
		long timestamp = getLocalTimestamp(s, i);
		Message sent = sendDeclineMessage(txn, i, timestamp, false);

		// Create the next state
		IntroducerState state = START;
		Introducee introduceeA, introduceeB;
		Author sender, other;
		if (senderIsAlice) {
			if (s.getState() == AWAIT_RESPONSES) state = A_DECLINED;
			introduceeA = new Introducee(s.getIntroduceeA(), m.getMessageId());
			introduceeB = new Introducee(s.getIntroduceeB(), sent);
			sender = introduceeA.author;
			other = introduceeB.author;
		} else {
			if (s.getState() == AWAIT_RESPONSES) state = B_DECLINED;
			introduceeA = new Introducee(s.getIntroduceeA(), sent);
			introduceeB = new Introducee(s.getIntroduceeB(), m.getMessageId());
			sender = introduceeB.author;
			other = introduceeA.author;
		}

		// Broadcast IntroductionResponseReceivedEvent
		broadcastIntroductionResponseReceivedEvent(txn, s, sender.getId(),
				other, m);

		return new IntroducerSession(s.getSessionId(), state,
				s.getRequestTimestamp(), introduceeA, introduceeB);
	}

	private IntroducerSession onRemoteDeclineWhenDeclined(Transaction txn,
			IntroducerSession s, DeclineMessage m) throws DbException {
		// The timestamp must be higher than the last request message
		if (m.getTimestamp() <= s.getRequestTimestamp())
			return abort(txn, s);
		// The dependency, if any, must be the last remote message
		if (isInvalidDependency(s, m.getGroupId(), m.getPreviousMessageId()))
			return abort(txn, s);
		// The message must be expected in the current state
		boolean senderIsAlice = senderIsAlice(s, m);
		if (senderIsAlice && s.getState() != B_DECLINED)
			return abort(txn, s);
		else if (!senderIsAlice && s.getState() != A_DECLINED)
			return abort(txn, s);

		// Mark the response visible in the UI
		markMessageVisibleInUi(txn, m.getMessageId());
		// Track the incoming message
		messageTracker
				.trackMessage(txn, m.getGroupId(), m.getTimestamp(), false);

		// Forward DECLINE message
		Introducee i = getOtherIntroducee(s, m.getGroupId());
		long timestamp = getLocalTimestamp(s, i);
		Message sent = sendDeclineMessage(txn, i, timestamp, false);

		Introducee introduceeA, introduceeB;
		Author sender, other;
		if (senderIsAlice) {
			introduceeA = new Introducee(s.getIntroduceeA(), m.getMessageId());
			introduceeB = new Introducee(s.getIntroduceeB(), sent);
			sender = introduceeA.author;
			other = introduceeB.author;
		} else {
			introduceeA = new Introducee(s.getIntroduceeA(), sent);
			introduceeB = new Introducee(s.getIntroduceeB(), m.getMessageId());
			sender = introduceeB.author;
			other = introduceeA.author;
		}

		// Broadcast IntroductionResponseReceivedEvent
		broadcastIntroductionResponseReceivedEvent(txn, s, sender.getId(),
				other, m);

		return new IntroducerSession(s.getSessionId(), START,
				s.getRequestTimestamp(), introduceeA, introduceeB);
	}

	private IntroducerSession onRemoteAuth(Transaction txn,
			IntroducerSession s, AuthMessage m) throws DbException {
		// The dependency, if any, must be the last remote message
		if (isInvalidDependency(s, m.getGroupId(), m.getPreviousMessageId()))
			return abort(txn, s);
		// The message must be expected in the current state
		boolean senderIsAlice = senderIsAlice(s, m);
		if (s.getState() != AWAIT_AUTHS) {
			if (senderIsAlice && s.getState() != AWAIT_AUTH_A)
				return abort(txn, s);
			else if (!senderIsAlice && s.getState() != AWAIT_AUTH_B)
				return abort(txn, s);
		}

		// Forward AUTH message
		Introducee i = getOtherIntroducee(s, m.getGroupId());
		long timestamp = getLocalTimestamp(s, i);
		Message sent = sendAuthMessage(txn, i, timestamp, m.getMac(),
				m.getSignature());

		// Move to the next state
		IntroducerState state = AWAIT_ACTIVATES;
		Introducee introduceeA, introduceeB;
		if (senderIsAlice) {
			if (s.getState() == AWAIT_AUTHS) state = AWAIT_AUTH_B;
			introduceeA = new Introducee(s.getIntroduceeA(), m.getMessageId());
			introduceeB = new Introducee(s.getIntroduceeB(), sent);
		} else {
			if (s.getState() == AWAIT_AUTHS) state = AWAIT_AUTH_A;
			introduceeA = new Introducee(s.getIntroduceeA(), sent);
			introduceeB = new Introducee(s.getIntroduceeB(), m.getMessageId());
		}
		return new IntroducerSession(s.getSessionId(), state,
				s.getRequestTimestamp(), introduceeA, introduceeB);
	}

	private IntroducerSession onRemoteActivate(Transaction txn,
			IntroducerSession s, ActivateMessage m) throws DbException {
		// The dependency, if any, must be the last remote message
		if (isInvalidDependency(s, m.getGroupId(), m.getPreviousMessageId()))
			return abort(txn, s);
		// The message must be expected in the current state
		boolean senderIsAlice = senderIsAlice(s, m);
		if (s.getState() != AWAIT_ACTIVATES) {
			if (senderIsAlice && s.getState() != AWAIT_ACTIVATE_A)
				return abort(txn, s);
			else if (!senderIsAlice && s.getState() != AWAIT_ACTIVATE_B)
				return abort(txn, s);
		}

		// Forward ACTIVATE message
		Introducee i = getOtherIntroducee(s, m.getGroupId());
		long timestamp = getLocalTimestamp(s, i);
		Message sent = sendActivateMessage(txn, i, timestamp, m.getMac());

		// Move to the next state
		IntroducerState state = START;
		Introducee introduceeA, introduceeB;
		if (senderIsAlice) {
			if (s.getState() == AWAIT_ACTIVATES) state = AWAIT_ACTIVATE_B;
			introduceeA = new Introducee(s.getIntroduceeA(), m.getMessageId());
			introduceeB = new Introducee(s.getIntroduceeB(), sent);
		} else {
			if (s.getState() == AWAIT_ACTIVATES) state = AWAIT_ACTIVATE_A;
			introduceeA = new Introducee(s.getIntroduceeA(), sent);
			introduceeB = new Introducee(s.getIntroduceeB(), m.getMessageId());
		}
		return new IntroducerSession(s.getSessionId(), state,
				s.getRequestTimestamp(), introduceeA, introduceeB);
	}

	private IntroducerSession onRemoteAbort(Transaction txn,
			IntroducerSession s, AbortMessage m) throws DbException {
		// Forward ABORT message
		Introducee i = getOtherIntroducee(s, m.getGroupId());
		long timestamp = getLocalTimestamp(s, i);
		Message sent = sendAbortMessage(txn, i, timestamp);

		// Broadcast abort event for testing
		txn.attach(new IntroductionAbortedEvent(s.getSessionId()));

		// Reset the session back to initial state
		Introducee introduceeA, introduceeB;
		if (i.equals(s.getIntroduceeA())) {
			introduceeA = new Introducee(s.getIntroduceeA(), sent);
			introduceeB = new Introducee(s.getIntroduceeB(), m.getMessageId());
		} else if (i.equals(s.getIntroduceeB())) {
			introduceeA = new Introducee(s.getIntroduceeA(), m.getMessageId());
			introduceeB = new Introducee(s.getIntroduceeB(), sent);
		} else throw new AssertionError();
		return new IntroducerSession(s.getSessionId(), START,
				s.getRequestTimestamp(), introduceeA, introduceeB);
	}

	private IntroducerSession abort(Transaction txn,
			IntroducerSession s) throws DbException {
		// Broadcast abort event for testing
		txn.attach(new IntroductionAbortedEvent(s.getSessionId()));

		// Send an ABORT message to both introducees
		long timestampA = getLocalTimestamp(s, s.getIntroduceeA());
		Message sentA = sendAbortMessage(txn, s.getIntroduceeA(), timestampA);
		long timestampB = getLocalTimestamp(s, s.getIntroduceeB());
		Message sentB = sendAbortMessage(txn, s.getIntroduceeB(), timestampB);
		// Reset the session back to initial state
		Introducee introduceeA = new Introducee(s.getIntroduceeA(), sentA);
		Introducee introduceeB = new Introducee(s.getIntroduceeB(), sentB);
		return new IntroducerSession(s.getSessionId(), START,
				s.getRequestTimestamp(), introduceeA, introduceeB);
	}

	private Introducee getIntroducee(IntroducerSession s, GroupId g) {
		if (s.getIntroduceeA().groupId.equals(g)) return s.getIntroduceeA();
		else if (s.getIntroduceeB().groupId.equals(g))
			return s.getIntroduceeB();
		else throw new AssertionError();
	}

	private Introducee getOtherIntroducee(IntroducerSession s, GroupId g) {
		if (s.getIntroduceeA().groupId.equals(g)) return s.getIntroduceeB();
		else if (s.getIntroduceeB().groupId.equals(g))
			return s.getIntroduceeA();
		else throw new AssertionError();
	}

	private boolean isInvalidDependency(IntroducerSession session,
			GroupId contactGroupId, @Nullable MessageId dependency) {
		MessageId expected =
				getIntroducee(session, contactGroupId).lastRemoteMessageId;
		return isInvalidDependency(expected, dependency);
	}

	private long getLocalTimestamp(IntroducerSession s, PeerSession p) {
		return getLocalTimestamp(p.getLocalTimestamp(),
				s.getRequestTimestamp());
	}

}

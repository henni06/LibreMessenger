package org.briarproject.sync;

import org.briarproject.api.ContactId;
import org.briarproject.api.FormatException;
import org.briarproject.api.TransportId;
import org.briarproject.api.db.DatabaseComponent;
import org.briarproject.api.db.DbException;
import org.briarproject.api.event.ContactRemovedEvent;
import org.briarproject.api.event.Event;
import org.briarproject.api.event.EventBus;
import org.briarproject.api.event.EventListener;
import org.briarproject.api.event.ShutdownEvent;
import org.briarproject.api.event.TransportRemovedEvent;
import org.briarproject.api.sync.Ack;
import org.briarproject.api.sync.Message;
import org.briarproject.api.sync.MessageVerifier;
import org.briarproject.api.sync.MessagingSession;
import org.briarproject.api.sync.Offer;
import org.briarproject.api.sync.PacketReader;
import org.briarproject.api.sync.Request;
import org.briarproject.api.sync.SubscriptionAck;
import org.briarproject.api.sync.SubscriptionUpdate;
import org.briarproject.api.sync.TransportAck;
import org.briarproject.api.sync.TransportUpdate;
import org.briarproject.api.sync.UnverifiedMessage;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

/**
 * An incoming {@link org.briarproject.api.sync.MessagingSession
 * MessagingSession}.
 */
class IncomingSession implements MessagingSession, EventListener {

	private static final Logger LOG =
			Logger.getLogger(IncomingSession.class.getName());

	private final DatabaseComponent db;
	private final Executor dbExecutor, cryptoExecutor;
	private final EventBus eventBus;
	private final MessageVerifier messageVerifier;
	private final ContactId contactId;
	private final TransportId transportId;
	private final PacketReader packetReader;

	private volatile boolean interrupted = false;

	IncomingSession(DatabaseComponent db, Executor dbExecutor,
			Executor cryptoExecutor, EventBus eventBus,
			MessageVerifier messageVerifier, ContactId contactId,
			TransportId transportId, PacketReader packetReader) {
		this.db = db;
		this.dbExecutor = dbExecutor;
		this.cryptoExecutor = cryptoExecutor;
		this.eventBus = eventBus;
		this.messageVerifier = messageVerifier;
		this.contactId = contactId;
		this.transportId = transportId;
		this.packetReader = packetReader;
	}

	public void run() throws IOException {
		eventBus.addListener(this);
		try {
			// Read packets until interrupted or EOF
			while (!interrupted && !packetReader.eof()) {
				if (packetReader.hasAck()) {
					Ack a = packetReader.readAck();
					dbExecutor.execute(new ReceiveAck(a));
				} else if (packetReader.hasMessage()) {
					UnverifiedMessage m = packetReader.readMessage();
					cryptoExecutor.execute(new VerifyMessage(m));
				} else if (packetReader.hasOffer()) {
					Offer o = packetReader.readOffer();
					dbExecutor.execute(new ReceiveOffer(o));
				} else if (packetReader.hasRequest()) {
					Request r = packetReader.readRequest();
					dbExecutor.execute(new ReceiveRequest(r));
				} else if (packetReader.hasSubscriptionAck()) {
					SubscriptionAck a = packetReader.readSubscriptionAck();
					dbExecutor.execute(new ReceiveSubscriptionAck(a));
				} else if (packetReader.hasSubscriptionUpdate()) {
					SubscriptionUpdate u = packetReader.readSubscriptionUpdate();
					dbExecutor.execute(new ReceiveSubscriptionUpdate(u));
				} else if (packetReader.hasTransportAck()) {
					TransportAck a = packetReader.readTransportAck();
					dbExecutor.execute(new ReceiveTransportAck(a));
				} else if (packetReader.hasTransportUpdate()) {
					TransportUpdate u = packetReader.readTransportUpdate();
					dbExecutor.execute(new ReceiveTransportUpdate(u));
				} else {
					throw new FormatException();
				}
			}
		} finally {
			eventBus.removeListener(this);
		}
	}

	public void interrupt() {
		// FIXME: This won't interrupt a blocking read
		interrupted = true;
	}

	public void eventOccurred(Event e) {
		if (e instanceof ContactRemovedEvent) {
			ContactRemovedEvent c = (ContactRemovedEvent) e;
			if (c.getContactId().equals(contactId)) interrupt();
		} else if (e instanceof ShutdownEvent) {
			interrupt();
		} else if (e instanceof TransportRemovedEvent) {
			TransportRemovedEvent t = (TransportRemovedEvent) e;
			if (t.getTransportId().equals(transportId)) interrupt();
		}
	}

	private class ReceiveAck implements Runnable {

		private final Ack ack;

		private ReceiveAck(Ack ack) {
			this.ack = ack;
		}

		public void run() {
			try {
				db.receiveAck(contactId, ack);
			} catch (DbException e) {
				if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
				interrupt();
			}
		}
	}

	private class VerifyMessage implements Runnable {

		private final UnverifiedMessage message;

		private VerifyMessage(UnverifiedMessage message) {
			this.message = message;
		}

		public void run() {
			try {
				Message m = messageVerifier.verifyMessage(message);
				dbExecutor.execute(new ReceiveMessage(m));
			} catch (GeneralSecurityException e) {
				if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
				interrupt();
			}
		}
	}

	private class ReceiveMessage implements Runnable {

		private final Message message;

		private ReceiveMessage(Message message) {
			this.message = message;
		}

		public void run() {
			try {
				db.receiveMessage(contactId, message);
			} catch (DbException e) {
				if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
				interrupt();
			}
		}
	}

	private class ReceiveOffer implements Runnable {

		private final Offer offer;

		private ReceiveOffer(Offer offer) {
			this.offer = offer;
		}

		public void run() {
			try {
				db.receiveOffer(contactId, offer);
			} catch (DbException e) {
				if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
				interrupt();
			}
		}
	}

	private class ReceiveRequest implements Runnable {

		private final Request request;

		private ReceiveRequest(Request request) {
			this.request = request;
		}

		public void run() {
			try {
				db.receiveRequest(contactId, request);
			} catch (DbException e) {
				if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
				interrupt();
			}
		}
	}

	private class ReceiveSubscriptionAck implements Runnable {

		private final SubscriptionAck ack;

		private ReceiveSubscriptionAck(SubscriptionAck ack) {
			this.ack = ack;
		}

		public void run() {
			try {
				db.receiveSubscriptionAck(contactId, ack);
			} catch (DbException e) {
				if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
				interrupt();
			}
		}
	}

	private class ReceiveSubscriptionUpdate implements Runnable {

		private final SubscriptionUpdate update;

		private ReceiveSubscriptionUpdate(SubscriptionUpdate update) {
			this.update = update;
		}

		public void run() {
			try {
				db.receiveSubscriptionUpdate(contactId, update);
			} catch (DbException e) {
				if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
				interrupt();
			}
		}
	}

	private class ReceiveTransportAck implements Runnable {

		private final TransportAck ack;

		private ReceiveTransportAck(TransportAck ack) {
			this.ack = ack;
		}

		public void run() {
			try {
				db.receiveTransportAck(contactId, ack);
			} catch (DbException e) {
				if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
				interrupt();
			}
		}
	}

	private class ReceiveTransportUpdate implements Runnable {

		private final TransportUpdate update;

		private ReceiveTransportUpdate(TransportUpdate update) {
			this.update = update;
		}

		public void run() {
			try {
				db.receiveTransportUpdate(contactId, update);
			} catch (DbException e) {
				if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
				interrupt();
			}
		}
	}
}

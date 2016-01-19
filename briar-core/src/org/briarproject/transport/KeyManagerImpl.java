package org.briarproject.transport;

import org.briarproject.api.TransportId;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.crypto.CryptoComponent;
import org.briarproject.api.crypto.SecretKey;
import org.briarproject.api.db.DatabaseComponent;
import org.briarproject.api.db.DatabaseExecutor;
import org.briarproject.api.db.DbException;
import org.briarproject.api.event.ContactRemovedEvent;
import org.briarproject.api.event.Event;
import org.briarproject.api.event.EventListener;
import org.briarproject.api.event.TransportAddedEvent;
import org.briarproject.api.event.TransportRemovedEvent;
import org.briarproject.api.lifecycle.Service;
import org.briarproject.api.system.Clock;
import org.briarproject.api.system.Timer;
import org.briarproject.api.transport.KeyManager;
import org.briarproject.api.transport.StreamContext;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import javax.inject.Inject;

import static java.util.logging.Level.WARNING;

class KeyManagerImpl implements KeyManager, Service, EventListener {

	private static final Logger LOG =
			Logger.getLogger(KeyManagerImpl.class.getName());

	private final DatabaseComponent db;
	private final CryptoComponent crypto;
	private final ExecutorService dbExecutor;
	private final Timer timer;
	private final Clock clock;
	private final ConcurrentHashMap<TransportId, TransportKeyManager> managers;

	@Inject
	KeyManagerImpl(DatabaseComponent db, CryptoComponent crypto,
			@DatabaseExecutor ExecutorService dbExecutor, Timer timer,
			Clock clock) {
		this.db = db;
		this.crypto = crypto;
		this.dbExecutor = dbExecutor;
		this.timer = timer;
		this.clock = clock;
		managers = new ConcurrentHashMap<TransportId, TransportKeyManager>();
	}

	@Override
	public boolean start() {
		try {
			Map<TransportId, Integer> latencies = db.getTransportLatencies();
			for (Entry<TransportId, Integer> e : latencies.entrySet())
				addTransport(e.getKey(), e.getValue());
		} catch (DbException e) {
			if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
			return false;
		}
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	public void addContact(ContactId c, Collection<TransportId> transports,
			SecretKey master, long timestamp, boolean alice) {
		for (TransportId t : transports) {
			TransportKeyManager m = managers.get(t);
			if (m != null) m.addContact(c, master, timestamp, alice);
		}
	}

	public StreamContext getStreamContext(ContactId c, TransportId t) {
		TransportKeyManager m = managers.get(t);
		return m == null ? null : m.getStreamContext(c);
	}

	public StreamContext getStreamContext(TransportId t, byte[] tag) {
		TransportKeyManager m = managers.get(t);
		return m == null ? null : m.recogniseTag(tag);
	}

	public void eventOccurred(Event e) {
		if (e instanceof TransportAddedEvent) {
			TransportAddedEvent t = (TransportAddedEvent) e;
			addTransport(t.getTransportId(), t.getMaxLatency());
		} else if (e instanceof TransportRemovedEvent) {
			removeTransport(((TransportRemovedEvent) e).getTransportId());
		} else if (e instanceof ContactRemovedEvent) {
			removeContact(((ContactRemovedEvent) e).getContactId());
		}
	}

	private void addTransport(final TransportId t, final int maxLatency) {
		dbExecutor.execute(new Runnable() {
			public void run() {
				TransportKeyManager m = new TransportKeyManager(db, crypto,
						dbExecutor, timer, clock, t, maxLatency);
				// Don't add transport twice if event is received during startup
				if (managers.putIfAbsent(t, m) == null) m.start();
			}
		});
	}

	private void removeTransport(TransportId t) {
		managers.remove(t);
	}

	private void removeContact(final ContactId c) {
		dbExecutor.execute(new Runnable() {
			public void run() {
				for (TransportKeyManager m : managers.values())
					m.removeContact(c);
			}
		});
	}
}

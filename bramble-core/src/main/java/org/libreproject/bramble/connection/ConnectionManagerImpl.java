package org.libreproject.bramble.connection;

import org.libreproject.bramble.api.connection.ConnectionManager;
import org.libreproject.bramble.api.connection.ConnectionRegistry;
import org.libreproject.bramble.api.contact.ContactExchangeManager;
import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.contact.HandshakeManager;
import org.libreproject.bramble.api.contact.PendingContactId;
import org.libreproject.bramble.api.lifecycle.IoExecutor;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportConnectionReader;
import org.libreproject.bramble.api.plugin.TransportConnectionWriter;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.plugin.duplex.DuplexTransportConnection;
import org.libreproject.bramble.api.properties.TransportPropertyManager;
import org.libreproject.bramble.api.sync.SyncSessionFactory;
import org.libreproject.bramble.api.transport.KeyManager;
import org.libreproject.bramble.api.transport.StreamReaderFactory;
import org.libreproject.bramble.api.transport.StreamWriterFactory;

import java.security.SecureRandom;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
@NotNullByDefault
class ConnectionManagerImpl implements ConnectionManager {

	private final Executor ioExecutor;
	private final KeyManager keyManager;
	private final StreamReaderFactory streamReaderFactory;
	private final StreamWriterFactory streamWriterFactory;
	private final SyncSessionFactory syncSessionFactory;
	private final HandshakeManager handshakeManager;
	private final ContactExchangeManager contactExchangeManager;
	private final ConnectionRegistry connectionRegistry;
	private final TransportPropertyManager transportPropertyManager;
	private final SecureRandom secureRandom;

	@Inject
	ConnectionManagerImpl(@IoExecutor Executor ioExecutor,
			KeyManager keyManager, StreamReaderFactory streamReaderFactory,
			StreamWriterFactory streamWriterFactory,
			SyncSessionFactory syncSessionFactory,
			HandshakeManager handshakeManager,
			ContactExchangeManager contactExchangeManager,
			ConnectionRegistry connectionRegistry,
			TransportPropertyManager transportPropertyManager,
			SecureRandom secureRandom) {
		this.ioExecutor = ioExecutor;
		this.keyManager = keyManager;
		this.streamReaderFactory = streamReaderFactory;
		this.streamWriterFactory = streamWriterFactory;
		this.syncSessionFactory = syncSessionFactory;
		this.handshakeManager = handshakeManager;
		this.contactExchangeManager = contactExchangeManager;
		this.connectionRegistry = connectionRegistry;
		this.transportPropertyManager = transportPropertyManager;
		this.secureRandom = secureRandom;
	}


	@Override
	public void manageIncomingConnection(TransportId t,
			TransportConnectionReader r) {
		ioExecutor.execute(new IncomingSimplexSyncConnection(keyManager,
				connectionRegistry, streamReaderFactory, streamWriterFactory,
				syncSessionFactory, transportPropertyManager, t, r));
	}

	@Override
	public void manageIncomingConnection(TransportId t,
			DuplexTransportConnection d) {
		ioExecutor.execute(new IncomingDuplexSyncConnection(keyManager,
				connectionRegistry, streamReaderFactory, streamWriterFactory,
				syncSessionFactory, transportPropertyManager, ioExecutor,
				t, d));
	}

	@Override
	public void manageIncomingConnection(PendingContactId p, TransportId t,
			DuplexTransportConnection d) {
		ioExecutor.execute(new IncomingHandshakeConnection(keyManager,
				connectionRegistry, streamReaderFactory, streamWriterFactory,
				handshakeManager, contactExchangeManager, this, p, t, d));
	}

	@Override
	public void manageOutgoingConnection(ContactId c, TransportId t,
			TransportConnectionWriter w) {
		ioExecutor.execute(new OutgoingSimplexSyncConnection(keyManager,
				connectionRegistry, streamReaderFactory, streamWriterFactory,
				syncSessionFactory, transportPropertyManager, c, t, w));
	}

	@Override
	public void manageOutgoingConnection(ContactId c, TransportId t,
			DuplexTransportConnection d) {
		ioExecutor.execute(new OutgoingDuplexSyncConnection(keyManager,
				connectionRegistry, streamReaderFactory, streamWriterFactory,
				syncSessionFactory, transportPropertyManager, ioExecutor,
				secureRandom, c, t, d));
	}

	@Override
	public void manageOutgoingConnection(PendingContactId p, TransportId t,
			DuplexTransportConnection d) {
		ioExecutor.execute(new OutgoingHandshakeConnection(keyManager,
				connectionRegistry, streamReaderFactory, streamWriterFactory,
				handshakeManager, contactExchangeManager, this, p, t, d));
	}
}

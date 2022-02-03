package org.libreproject.bramble.connection;

import org.libreproject.bramble.api.connection.ConnectionRegistry;
import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportConnectionWriter;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.properties.TransportPropertyManager;
import org.libreproject.bramble.api.sync.SyncSession;
import org.libreproject.bramble.api.sync.SyncSessionFactory;
import org.libreproject.bramble.api.transport.KeyManager;
import org.libreproject.bramble.api.transport.StreamContext;
import org.libreproject.bramble.api.transport.StreamReaderFactory;
import org.libreproject.bramble.api.transport.StreamWriter;
import org.libreproject.bramble.api.transport.StreamWriterFactory;

import java.io.IOException;

import static java.util.logging.Level.WARNING;
import static org.libreproject.bramble.api.nullsafety.NullSafety.requireNonNull;
import static org.libreproject.bramble.util.LogUtils.logException;

@NotNullByDefault
class OutgoingSimplexSyncConnection extends SyncConnection implements Runnable {

	private final ContactId contactId;
	private final TransportId transportId;
	private final TransportConnectionWriter writer;

	OutgoingSimplexSyncConnection(KeyManager keyManager,
			ConnectionRegistry connectionRegistry,
			StreamReaderFactory streamReaderFactory,
			StreamWriterFactory streamWriterFactory,
			SyncSessionFactory syncSessionFactory,
			TransportPropertyManager transportPropertyManager,
			ContactId contactId, TransportId transportId,
			TransportConnectionWriter writer) {
		super(keyManager, connectionRegistry, streamReaderFactory,
				streamWriterFactory, syncSessionFactory,
				transportPropertyManager);
		this.contactId = contactId;
		this.transportId = transportId;
		this.writer = writer;
	}

	@Override
	public void run() {
		// Allocate a stream context
		StreamContext ctx = allocateStreamContext(contactId, transportId);
		if (ctx == null) {
			LOG.warning("Could not allocate stream context");
			onError();
			return;
		}
		try {
			// Create and run the outgoing session
			createSimplexOutgoingSession(ctx, writer).run();
			writer.dispose(false);
		} catch (IOException e) {
			logException(LOG, WARNING, e);
			onError();
		}
	}

	private void onError() {
		disposeOnError(writer);
	}

	private SyncSession createSimplexOutgoingSession(StreamContext ctx,
			TransportConnectionWriter w) throws IOException {
		StreamWriter streamWriter = streamWriterFactory.createStreamWriter(
				w.getOutputStream(), ctx);
		ContactId c = requireNonNull(ctx.getContactId());
		// Use eager retransmission if the transport is lossy and cheap
		return syncSessionFactory.createSimplexOutgoingSession(c,
				ctx.getTransportId(), w.getMaxLatency(), w.isLossyAndCheap(),
				streamWriter);
	}
}


package org.briarproject.bramble.plugin;

import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.plugin.ConnectionRegistry;
import org.briarproject.bramble.api.plugin.TransportConnectionReader;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.properties.TransportPropertyManager;
import org.briarproject.bramble.api.sync.SyncSessionFactory;
import org.briarproject.bramble.api.transport.KeyManager;
import org.briarproject.bramble.api.transport.StreamContext;
import org.briarproject.bramble.api.transport.StreamReaderFactory;
import org.briarproject.bramble.api.transport.StreamWriterFactory;

import java.io.IOException;

import static java.util.logging.Level.WARNING;
import static org.briarproject.bramble.util.LogUtils.logException;

@NotNullByDefault
class IncomingSimplexSyncConnection extends SyncConnection implements Runnable {

	private final TransportId transportId;
	private final TransportConnectionReader reader;

	IncomingSimplexSyncConnection(KeyManager keyManager,
			ConnectionRegistry connectionRegistry,
			StreamReaderFactory streamReaderFactory,
			StreamWriterFactory streamWriterFactory,
			SyncSessionFactory syncSessionFactory,
			TransportPropertyManager transportPropertyManager,
			TransportId transportId, TransportConnectionReader reader) {
		super(keyManager, connectionRegistry, streamReaderFactory,
				streamWriterFactory, syncSessionFactory,
				transportPropertyManager);
		this.transportId = transportId;
		this.reader = reader;
	}

	@Override
	public void run() {
		// Read and recognise the tag
		StreamContext ctx = recogniseTag(reader, transportId);
		if (ctx == null) {
			LOG.info("Unrecognised tag");
			onError(false);
			return;
		}
		ContactId contactId = ctx.getContactId();
		if (contactId == null) {
			LOG.warning("Received rendezvous stream, expected contact");
			onError(true);
			return;
		}
		if (ctx.isHandshakeMode()) {
			// TODO: Support handshake mode for contacts
			LOG.warning("Received handshake tag, expected rotation mode");
			onError(true);
			return;
		}
		connectionRegistry.registerConnection(contactId, transportId, true);
		try {
			// Create and run the incoming session
			createIncomingSession(ctx, reader).run();
			reader.dispose(false, true);
		} catch (IOException e) {
			logException(LOG, WARNING, e);
			onError(true);
		} finally {
			connectionRegistry.unregisterConnection(contactId, transportId,
					true);
		}
	}

	private void onError(boolean recognised) {
		disposeOnError(reader, recognised);
	}
}


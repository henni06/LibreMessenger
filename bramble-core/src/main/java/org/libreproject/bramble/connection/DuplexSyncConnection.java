package org.libreproject.bramble.connection;

import org.libreproject.bramble.api.connection.ConnectionRegistry;
import org.libreproject.bramble.api.connection.InterruptibleConnection;
import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportConnectionReader;
import org.libreproject.bramble.api.plugin.TransportConnectionWriter;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.plugin.duplex.DuplexTransportConnection;
import org.libreproject.bramble.api.properties.TransportProperties;
import org.libreproject.bramble.api.properties.TransportPropertyManager;
import org.libreproject.bramble.api.sync.Priority;
import org.libreproject.bramble.api.sync.SyncSession;
import org.libreproject.bramble.api.sync.SyncSessionFactory;
import org.libreproject.bramble.api.transport.KeyManager;
import org.libreproject.bramble.api.transport.StreamContext;
import org.libreproject.bramble.api.transport.StreamReaderFactory;
import org.libreproject.bramble.api.transport.StreamWriter;
import org.libreproject.bramble.api.transport.StreamWriterFactory;

import java.io.IOException;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import static org.libreproject.bramble.api.nullsafety.NullSafety.requireNonNull;

@NotNullByDefault
abstract class DuplexSyncConnection extends SyncConnection
		implements InterruptibleConnection {

	final Executor ioExecutor;
	final TransportId transportId;
	final TransportConnectionReader reader;
	final TransportConnectionWriter writer;
	final TransportProperties remote;

	private final Object interruptLock = new Object();

	@GuardedBy("interruptLock")
	@Nullable
	private SyncSession outgoingSession = null;
	@GuardedBy("interruptLock")
	private boolean interruptWaiting = false;

	@Override
	public void interruptOutgoingSession() {
		SyncSession out = null;
		synchronized (interruptLock) {
			if (outgoingSession == null) interruptWaiting = true;
			else out = outgoingSession;
		}
		if (out != null) out.interrupt();
	}

	void setOutgoingSession(SyncSession outgoingSession) {
		boolean interruptWasWaiting = false;
		synchronized (interruptLock) {
			this.outgoingSession = outgoingSession;
			if (interruptWaiting) {
				interruptWasWaiting = true;
				interruptWaiting = false;
			}
		}
		if (interruptWasWaiting) outgoingSession.interrupt();
	}

	DuplexSyncConnection(KeyManager keyManager,
			ConnectionRegistry connectionRegistry,
			StreamReaderFactory streamReaderFactory,
			StreamWriterFactory streamWriterFactory,
			SyncSessionFactory syncSessionFactory,
			TransportPropertyManager transportPropertyManager,
			Executor ioExecutor, TransportId transportId,
			DuplexTransportConnection connection) {
		super(keyManager, connectionRegistry, streamReaderFactory,
				streamWriterFactory, syncSessionFactory,
				transportPropertyManager);
		this.ioExecutor = ioExecutor;
		this.transportId = transportId;
		reader = connection.getReader();
		writer = connection.getWriter();
		remote = connection.getRemoteProperties();
	}

	void onReadError(boolean recognised) {
		disposeOnError(reader, recognised);
		disposeOnError(writer);
		interruptOutgoingSession();
	}

	void onWriteError() {
		disposeOnError(reader, true);
		disposeOnError(writer);
	}

	SyncSession createDuplexOutgoingSession(StreamContext ctx,
			TransportConnectionWriter w, @Nullable Priority priority)
			throws IOException {
		StreamWriter streamWriter = streamWriterFactory.createStreamWriter(
				w.getOutputStream(), ctx);
		ContactId c = requireNonNull(ctx.getContactId());
		return syncSessionFactory.createDuplexOutgoingSession(c,
				ctx.getTransportId(), w.getMaxLatency(), w.getMaxIdleTime(),
				streamWriter, priority);
	}
}

package org.libreproject.bramble.connection;

import org.libreproject.bramble.api.connection.ConnectionRegistry;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportConnectionReader;
import org.libreproject.bramble.api.plugin.TransportConnectionWriter;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.transport.KeyManager;
import org.libreproject.bramble.api.transport.StreamContext;
import org.libreproject.bramble.api.transport.StreamReaderFactory;
import org.libreproject.bramble.api.transport.StreamWriterFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.libreproject.bramble.api.transport.TransportConstants.TAG_LENGTH;
import static org.libreproject.bramble.util.IoUtils.read;
import static org.libreproject.bramble.util.LogUtils.logException;

@NotNullByDefault
abstract class Connection {

	protected static final Logger LOG = getLogger(Connection.class.getName());

	final KeyManager keyManager;
	final ConnectionRegistry connectionRegistry;
	final StreamReaderFactory streamReaderFactory;
	final StreamWriterFactory streamWriterFactory;

	Connection(KeyManager keyManager, ConnectionRegistry connectionRegistry,
			StreamReaderFactory streamReaderFactory,
			StreamWriterFactory streamWriterFactory) {
		this.keyManager = keyManager;
		this.connectionRegistry = connectionRegistry;
		this.streamReaderFactory = streamReaderFactory;
		this.streamWriterFactory = streamWriterFactory;
	}

	@Nullable
	StreamContext recogniseTag(TransportConnectionReader reader,
			TransportId transportId) {
		try {
			byte[] tag = readTag(reader.getInputStream());
			return keyManager.getStreamContext(transportId, tag);
		} catch (IOException | DbException e) {
			logException(LOG, WARNING, e);
			return null;
		}
	}

	private byte[] readTag(InputStream in) throws IOException {
		byte[] tag = new byte[TAG_LENGTH];
		read(in, tag);
		return tag;
	}

	void disposeOnError(TransportConnectionReader reader, boolean recognised) {
		try {
			reader.dispose(true, recognised);
		} catch (IOException e) {
			logException(LOG, WARNING, e);
		}
	}

	void disposeOnError(TransportConnectionWriter writer) {
		try {
			writer.dispose(true);
		} catch (IOException e) {
			logException(LOG, WARNING, e);
		}
	}
}

package org.libreproject.bramble.plugin.file;

import org.libreproject.bramble.api.connection.ConnectionManager;
import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.db.DatabaseComponent;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.event.EventListener;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.PluginManager;
import org.libreproject.bramble.api.plugin.TransportConnectionWriter;
import org.libreproject.bramble.api.plugin.simplex.SimplexPlugin;
import org.libreproject.bramble.api.properties.TransportProperties;
import org.libreproject.bramble.api.sync.event.MessagesSentEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.libreproject.bramble.api.plugin.file.RemovableDriveConstants.ID;
import static org.libreproject.bramble.util.LogUtils.logException;

@NotNullByDefault
class RemovableDriveWriterTask extends RemovableDriveTaskImpl
		implements EventListener {

	private static final Logger LOG =
			getLogger(RemovableDriveWriterTask.class.getName());

	private final DatabaseComponent db;
	private final ContactId contactId;

	RemovableDriveWriterTask(
			DatabaseComponent db,
			Executor eventExecutor,
			PluginManager pluginManager,
			ConnectionManager connectionManager,
			EventBus eventBus,
			RemovableDriveTaskRegistry registry,
			ContactId contactId,
			TransportProperties transportProperties) {
		super(eventExecutor, pluginManager, connectionManager, eventBus,
				registry, transportProperties);
		this.db = db;
		this.contactId = contactId;
	}

	@Override
	public void run() {
		SimplexPlugin plugin = getPlugin();
		TransportConnectionWriter w = plugin.createWriter(transportProperties);
		if (w == null) {
			LOG.warning("Failed to create writer");
			registry.removeWriter(this);
			setSuccess(false);
			return;
		}
		try {
			setTotal(db.transactionWithResult(true, txn ->
					db.getUnackedMessageBytesToSend(txn, contactId)));
		} catch (DbException e) {
			logException(LOG, WARNING, e);
			registry.removeWriter(this);
			setSuccess(false);
			return;
		}
		eventBus.addListener(this);
		connectionManager.manageOutgoingConnection(contactId, ID,
				new DecoratedWriter(w));
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof MessagesSentEvent) {
			MessagesSentEvent m = (MessagesSentEvent) e;
			if (contactId.equals(m.getContactId())) {
				if (LOG.isLoggable(INFO)) {
					LOG.info(m.getMessageIds().size() + " messages sent");
				}
				addDone(m.getTotalLength());
			}
		}
	}

	private class DecoratedWriter implements TransportConnectionWriter {

		private final TransportConnectionWriter delegate;

		private DecoratedWriter(TransportConnectionWriter delegate) {
			this.delegate = delegate;
		}

		@Override
		public long getMaxLatency() {
			return delegate.getMaxLatency();
		}

		@Override
		public int getMaxIdleTime() {
			return delegate.getMaxIdleTime();
		}

		@Override
		public boolean isLossyAndCheap() {
			return delegate.isLossyAndCheap();
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return delegate.getOutputStream();
		}

		@Override
		public void dispose(boolean exception) throws IOException {
			delegate.dispose(exception);
			registry.removeWriter(RemovableDriveWriterTask.this);
			eventBus.removeListener(RemovableDriveWriterTask.this);
			setSuccess(!exception);
		}
	}
}

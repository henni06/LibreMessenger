package org.libreproject.bramble.plugin.file;

import org.libreproject.bramble.api.connection.ConnectionManager;
import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.db.DatabaseComponent;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.event.EventExecutor;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.PluginManager;
import org.libreproject.bramble.api.plugin.file.RemovableDriveTask;
import org.libreproject.bramble.api.properties.TransportProperties;

import java.util.concurrent.Executor;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
@NotNullByDefault
class RemovableDriveTaskFactoryImpl implements RemovableDriveTaskFactory {

	private final DatabaseComponent db;
	private final Executor eventExecutor;
	private final PluginManager pluginManager;
	private final ConnectionManager connectionManager;
	private final EventBus eventBus;

	@Inject
	RemovableDriveTaskFactoryImpl(
			DatabaseComponent db,
			@EventExecutor Executor eventExecutor,
			PluginManager pluginManager,
			ConnectionManager connectionManager,
			EventBus eventBus) {
		this.db = db;
		this.eventExecutor = eventExecutor;
		this.pluginManager = pluginManager;
		this.connectionManager = connectionManager;
		this.eventBus = eventBus;
	}

	@Override
	public RemovableDriveTask createReader(RemovableDriveTaskRegistry registry,
			TransportProperties p) {
		return new RemovableDriveReaderTask(eventExecutor, pluginManager,
				connectionManager, eventBus, registry, p);
	}

	@Override
	public RemovableDriveTask createWriter(RemovableDriveTaskRegistry registry,
			ContactId c, TransportProperties p) {
		return new RemovableDriveWriterTask(db, eventExecutor, pluginManager,
				connectionManager, eventBus, registry, c, p);
	}
}

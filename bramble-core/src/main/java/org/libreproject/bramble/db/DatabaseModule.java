package org.libreproject.bramble.db;

import org.libreproject.bramble.api.db.DatabaseComponent;
import org.libreproject.bramble.api.db.DatabaseConfig;
import org.libreproject.bramble.api.db.TransactionManager;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.event.EventExecutor;
import org.libreproject.bramble.api.lifecycle.ShutdownManager;
import org.libreproject.bramble.api.sync.MessageFactory;
import org.libreproject.bramble.api.system.Clock;

import java.sql.Connection;
import java.util.concurrent.Executor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DatabaseModule {

	@Provides
	@Singleton
	Database<Connection> provideDatabase(DatabaseConfig config,
			MessageFactory messageFactory, Clock clock) {
		return new H2Database(config, messageFactory, clock);
	}

	@Provides
	@Singleton
	DatabaseComponent provideDatabaseComponent(Database<Connection> db,
			EventBus eventBus, @EventExecutor Executor eventExecutor,
			ShutdownManager shutdownManager) {
		return new DatabaseComponentImpl<>(db, Connection.class, eventBus,
				eventExecutor, shutdownManager);
	}

	@Provides
	TransactionManager provideTransactionManager(DatabaseComponent db) {
		return db;
	}
}

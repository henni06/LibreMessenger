package org.libreproject.bramble.db;

import org.libreproject.bramble.api.db.DatabaseConfig;
import org.libreproject.bramble.api.sync.MessageFactory;
import org.libreproject.bramble.api.system.Clock;
import org.junit.Ignore;

import java.sql.Connection;

@Ignore
public class H2HyperSqlDatabasePerformanceComparisonTest
		extends DatabasePerformanceComparisonTest {

	@Override
	Database<Connection> createDatabase(boolean conditionA,
			DatabaseConfig databaseConfig, MessageFactory messageFactory,
			Clock clock) {
		if (conditionA)
			return new H2Database(databaseConfig, messageFactory, clock);
		else return new HyperSqlDatabase(databaseConfig, messageFactory, clock);
	}

	@Override
	protected String getTestName() {
		return getClass().getSimpleName();
	}
}

package org.libreproject.bramble.db;

import org.libreproject.bramble.api.db.DatabaseConfig;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.MessageFactory;
import org.libreproject.bramble.api.system.Clock;
import org.junit.Ignore;

import java.sql.Connection;

/**
 * Sanity check for {@link DatabasePerformanceComparisonTest}: check that
 * if condition B sleeps for 1ms before every commit, condition A is
 * considered to be faster.
 */
@Ignore
public class H2SleepDatabasePerformanceComparisonTest
		extends DatabasePerformanceComparisonTest {

	@Override
	Database<Connection> createDatabase(boolean conditionA,
			DatabaseConfig databaseConfig, MessageFactory messageFactory,
			Clock clock) {
		if (conditionA) {
			return new H2Database(databaseConfig, messageFactory, clock);
		} else {
			return new H2Database(databaseConfig, messageFactory, clock) {
				@Override
				@NotNullByDefault
				public void commitTransaction(Connection txn)
						throws DbException {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						throw new DbException(e);
					}
					super.commitTransaction(txn);
				}
			};
		}
	}

	@Override
	protected String getTestName() {
		return getClass().getSimpleName();
	}
}

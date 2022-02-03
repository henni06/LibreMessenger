package org.libreproject.bramble.db;

import org.libreproject.bramble.api.db.DatabaseConfig;
import org.libreproject.bramble.api.sync.MessageFactory;
import org.libreproject.bramble.api.system.Clock;
import org.junit.Ignore;

@Ignore
public class H2DatabasePerformanceTest extends SingleDatabasePerformanceTest {

	@Override
	protected String getTestName() {
		return getClass().getSimpleName();
	}

	@Override
	protected JdbcDatabase createDatabase(DatabaseConfig config,
			MessageFactory messageFactory, Clock clock) {
		return new H2Database(config, messageFactory, clock);
	}
}

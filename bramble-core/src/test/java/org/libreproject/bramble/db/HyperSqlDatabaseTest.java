package org.libreproject.bramble.db;

import org.libreproject.bramble.api.db.DatabaseConfig;
import org.libreproject.bramble.api.sync.MessageFactory;
import org.libreproject.bramble.api.system.Clock;

public class HyperSqlDatabaseTest extends JdbcDatabaseTest {

	@Override
	protected JdbcDatabase createDatabase(DatabaseConfig config,
			MessageFactory messageFactory, Clock clock) {
		return new HyperSqlDatabase(config, messageFactory ,clock);
	}
}

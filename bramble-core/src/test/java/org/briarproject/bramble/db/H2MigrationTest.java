package org.briarproject.bramble.db;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;

import java.sql.Connection;
import java.util.List;

@NotNullByDefault
public class H2MigrationTest extends DatabaseMigrationTest {

	@Override
	Database<Connection> createDatabase(
			List<Migration<Connection>> migrations) {
		return new H2Database(config, clock) {
			@Override
			List<Migration<Connection>> getMigrations() {
				return migrations;
			}
		};
	}
}

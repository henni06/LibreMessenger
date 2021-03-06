package org.libreproject.bramble.db;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.sql.Connection;
import java.util.List;

@NotNullByDefault
public class HyperSqlMigrationTest extends DatabaseMigrationTest {

	@Override
	Database<Connection> createDatabase(
			List<Migration<Connection>> migrations) {
		return new HyperSqlDatabase(config, messageFactory, clock) {
			@Override
			List<Migration<Connection>> getMigrations() {
				return migrations;
			}
		};
	}
}

package org.libreproject.bramble.db;

import org.libreproject.bramble.api.db.DbException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.libreproject.bramble.db.JdbcUtils.tryToClose;

class Migration40_41 implements Migration<Connection> {

	private static final Logger LOG = getLogger(Migration40_41.class.getName());

	private final DatabaseTypes dbTypes;

	Migration40_41(DatabaseTypes databaseTypes) {
		this.dbTypes = databaseTypes;
	}

	@Override
	public int getStartVersion() {
		return 40;
	}

	@Override
	public int getEndVersion() {
		return 41;
	}

	@Override
	public void migrate(Connection txn) throws DbException {
		Statement s = null;
		try {
			s = txn.createStatement();
			s.execute("ALTER TABLE contacts"
					+ dbTypes.replaceTypes(" ADD alias _STRING"));
		} catch (SQLException e) {
			tryToClose(s, LOG, WARNING);
			throw new DbException(e);
		}
	}
}

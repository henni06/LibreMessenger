package org.briarproject.bramble.db;

import org.briarproject.bramble.api.db.DbException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import static java.util.logging.Level.WARNING;
import static org.briarproject.bramble.util.LogUtils.logException;

class Migration38_39 implements Migration<Connection> {

	private static final Logger LOG =
			Logger.getLogger(Migration38_39.class.getName());

	@Override
	public int getStartVersion() {
		return 38;
	}

	@Override
	public int getEndVersion() {
		return 39;
	}

	@Override
	public void migrate(Connection txn) throws DbException {
		Statement s = null;
		try {
			s = txn.createStatement();
			// Add not null constraints
			s.execute("ALTER TABLE outgoingKeys"
					+ " ALTER COLUMN contactId"
					+ " SET NOT NULL");
			s.execute("ALTER TABLE incomingKeys"
					+ " ALTER COLUMN contactId"
					+ " SET NOT NULL");
		} catch (SQLException e) {
			tryToClose(s);
			throw new DbException(e);
		}
	}

	private void tryToClose(@Nullable Statement s) {
		try {
			if (s != null) s.close();
		} catch (SQLException e) {
			logException(LOG, WARNING, e);
		}
	}
}

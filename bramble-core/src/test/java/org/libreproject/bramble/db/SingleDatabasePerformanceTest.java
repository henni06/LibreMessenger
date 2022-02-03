package org.libreproject.bramble.db;

import org.libreproject.bramble.api.crypto.SecretKey;
import org.libreproject.bramble.api.db.DatabaseConfig;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.sync.MessageFactory;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.bramble.system.SystemClock;
import org.libreproject.bramble.test.TestDatabaseConfig;
import org.libreproject.bramble.test.TestMessageFactory;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

import static org.libreproject.bramble.test.TestUtils.deleteTestDirectory;
import static org.libreproject.bramble.test.TestUtils.getMean;
import static org.libreproject.bramble.test.TestUtils.getMedian;
import static org.libreproject.bramble.test.TestUtils.getSecretKey;
import static org.libreproject.bramble.test.TestUtils.getStandardDeviation;

public abstract class SingleDatabasePerformanceTest
		extends DatabasePerformanceTest {

	abstract Database<Connection> createDatabase(DatabaseConfig databaseConfig,
			MessageFactory messageFactory, Clock clock);

	private SecretKey databaseKey = getSecretKey();

	@Override
	protected void benchmark(String name,
			BenchmarkTask<Database<Connection>> task) throws Exception {
		deleteTestDirectory(testDir);
		Database<Connection> db = openDatabase();
		populateDatabase(db);
		db.close();
		db = openDatabase();
		// Measure the first iteration
		long firstDuration = measureOne(db, task);
		// Measure blocks of iterations until we reach a steady state
		SteadyStateResult result = measureSteadyState(db, task);
		db.close();
		writeResult(name, result.blocks, firstDuration, result.durations);
	}

	private Database<Connection> openDatabase() throws DbException {
		Database<Connection> db = createDatabase(
				new TestDatabaseConfig(testDir), new TestMessageFactory(),
				new SystemClock());
		db.open(databaseKey, null);
		return db;
	}

	private void writeResult(String name, int blocks, long firstDuration,
			List<Double> durations) throws IOException {
		String result = String.format("%s\t%d\t%,d\t%,d\t%,d\t%,d", name,
				blocks, firstDuration, (long) getMean(durations),
				(long) getMedian(durations),
				(long) getStandardDeviation(durations));
		writeResult(result);
	}
}

package org.briarproject.bramble.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.FINE;

public class LogUtils {

	private static final int NANOS_PER_MILLI = 1000 * 1000;

	/**
	 * Returns the elapsed time in milliseconds since some arbitrary
	 * starting time. This is only useful for measuring elapsed time.
	 */
	public static long now() {
		return System.nanoTime() / NANOS_PER_MILLI;
	}

	/**
	 * Logs the duration of a task.
	 * @param logger the logger to use
	 * @param task a description of the task
	 * @param start the start time of the task, as returned by {@link #now()}
	 */
	public static void logDuration(Logger logger, String task, long start) {
		if (logger.isLoggable(FINE)) {
			long duration = now() - start;
			logger.fine(task + " took " + duration + " ms");
		}
	}

	public static void logException(Logger logger, Level level, Throwable t) {
		if (logger.isLoggable(level)) logger.log(level, t.toString(), t);
	}
}

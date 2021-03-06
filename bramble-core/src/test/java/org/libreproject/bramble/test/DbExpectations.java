package org.libreproject.bramble.test;

import org.libreproject.bramble.api.db.DbCallable;
import org.libreproject.bramble.api.db.DbRunnable;
import org.libreproject.bramble.api.db.NullableDbCallable;
import org.libreproject.bramble.api.db.Transaction;
import org.jmock.Expectations;

public class DbExpectations extends Expectations {

	protected <E extends Exception> DbRunnable<E> withDbRunnable(
			Transaction txn) {
		addParameterMatcher(any(DbRunnable.class));
		currentBuilder().setAction(new RunTransactionAction(txn));
		return null;
	}

	protected <R, E extends Exception> DbCallable<R, E> withDbCallable(
			Transaction txn) {
		addParameterMatcher(any(DbCallable.class));
		currentBuilder().setAction(new RunTransactionWithResultAction(txn));
		return null;
	}

	protected <R, E extends Exception> NullableDbCallable<R, E> withNullableDbCallable(
			Transaction txn) {
		addParameterMatcher(any(NullableDbCallable.class));
		currentBuilder().setAction(
				new RunTransactionWithNullableResultAction(txn));
		return null;
	}

}

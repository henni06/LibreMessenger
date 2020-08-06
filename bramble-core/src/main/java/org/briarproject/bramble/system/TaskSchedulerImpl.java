package org.briarproject.bramble.system;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.system.TaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A {@link TaskScheduler} that uses a {@link ScheduledExecutorService}.
 */
@ThreadSafe
@NotNullByDefault
class TaskSchedulerImpl implements TaskScheduler {

	private final ScheduledExecutorService delegate;

	TaskSchedulerImpl(ScheduledExecutorService delegate) {
		this.delegate = delegate;
	}

	@Override
	public Future<?> schedule(Runnable task, Executor executor, long delay,
			TimeUnit unit) {
		Runnable execute = () -> executor.execute(task);
		return delegate.schedule(execute, delay, unit);
	}

	@Override
	public Future<?> scheduleWithFixedDelay(Runnable task, Executor executor,
			long delay, long interval, TimeUnit unit) {
		Runnable execute = () -> executor.execute(task);
		return delegate.scheduleWithFixedDelay(execute, delay, interval, unit);
	}
}

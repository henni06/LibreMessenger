package org.briarproject.plugins;

import org.briarproject.api.plugins.Backoff;

import java.util.concurrent.atomic.AtomicInteger;

class BackoffImpl implements Backoff {

	private final int minInterval, maxInterval;
	private final double base;
	private final AtomicInteger backoff;

	BackoffImpl(int minInterval, int maxInterval, double base) {
		this.minInterval = minInterval;
		this.maxInterval = maxInterval;
		this.base = base;
		backoff = new AtomicInteger(0);
	}

	@Override
	public int getPollingInterval() {
		double multiplier = Math.pow(base, backoff.get());
		// Large or infinite values will be rounded to Integer.MAX_VALUE
		int interval = (int) (minInterval * multiplier);
		return Math.min(interval, maxInterval);
	}

	@Override
	public void increment() {
		backoff.incrementAndGet();
	}

	@Override
	public void reset() {
		backoff.set(0);
	}
}

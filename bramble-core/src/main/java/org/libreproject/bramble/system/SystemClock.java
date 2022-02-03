package org.libreproject.bramble.system;

import org.libreproject.bramble.api.system.Clock;

/**
 * Default clock implementation.
 */
public class SystemClock implements Clock {

	@Override
	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	@Override
	public void sleep(long milliseconds) throws InterruptedException {
		Thread.sleep(milliseconds);
	}
}

package org.libreproject.bramble.plugin;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.Backoff;
import org.libreproject.bramble.api.plugin.BackoffFactory;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class BackoffFactoryImpl implements BackoffFactory {

	@Override
	public Backoff createBackoff(int minInterval, int maxInterval,
			double base) {
		return new BackoffImpl(minInterval, maxInterval, base);
	}
}

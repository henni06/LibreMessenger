package org.libreproject.bramble.api.reliability;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface ReliabilityLayerFactory {

	/**
	 * Returns a reliability layer that writes to the given lower layer.
	 */
	ReliabilityLayer createReliabilityLayer(WriteHandler writeHandler);
}

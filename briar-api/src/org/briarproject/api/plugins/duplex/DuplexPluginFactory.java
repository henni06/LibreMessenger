package org.briarproject.api.plugins.duplex;

import org.briarproject.api.TransportId;

/**
 * Factory for creating a plugin for a duplex transport.
 */
public interface DuplexPluginFactory {

	/**
	 * Returns the plugin's transport identifier.
	 */
	TransportId getId();

	/**
	 * Returns the maximum latency of the transport in milliseconds.
	 */
	int getMaxLatency();

	/**
	 * Creates and returns a plugin, or null if no plugin can be created.
	 */
	DuplexPlugin createPlugin(DuplexPluginCallback callback);
}

package org.briarproject.api.plugins;

import org.briarproject.api.TransportId;
import org.briarproject.api.plugins.duplex.DuplexPlugin;
import org.briarproject.api.plugins.simplex.SimplexPlugin;

import java.util.Collection;

/**
 * Responsible for starting transport plugins at startup and stopping them at
 * shutdown.
 */
public interface PluginManager {

	/**
	 * Returns the plugin for the given transport, or null if no such plugin
	 * has been created.
	 */
	Plugin getPlugin(TransportId t);

	/**
	 * Returns any simplex plugins that have been created.
	 */
	Collection<SimplexPlugin> getSimplexPlugins();

	/**
	 * Returns any duplex plugins that have been created.
	 */
	Collection<DuplexPlugin> getDuplexPlugins();

	/**
	 * Returns any duplex plugins that support invitations.
	 */
	Collection<DuplexPlugin> getInvitationPlugins();

	/**
	 * Returns any duplex plugins that support key agreement.
	 */
	Collection<DuplexPlugin> getKeyAgreementPlugins();
}

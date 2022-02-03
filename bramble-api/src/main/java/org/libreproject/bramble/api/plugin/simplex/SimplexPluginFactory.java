package org.libreproject.bramble.api.plugin.simplex;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.PluginFactory;

/**
 * Factory for creating a plugin for a simplex transport.
 */
@NotNullByDefault
public interface SimplexPluginFactory extends PluginFactory<SimplexPlugin> {
}

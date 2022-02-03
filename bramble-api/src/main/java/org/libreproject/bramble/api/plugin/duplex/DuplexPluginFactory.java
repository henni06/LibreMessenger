package org.libreproject.bramble.api.plugin.duplex;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.PluginFactory;

/**
 * Factory for creating a plugin for a duplex transport.
 */
@NotNullByDefault
public interface DuplexPluginFactory extends PluginFactory<DuplexPlugin> {
}

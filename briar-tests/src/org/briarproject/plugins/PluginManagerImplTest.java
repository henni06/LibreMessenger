package org.briarproject.plugins;

import org.briarproject.BriarTestCase;
import org.briarproject.api.TransportId;
import org.briarproject.api.event.EventBus;
import org.briarproject.api.plugins.ConnectionManager;
import org.briarproject.api.plugins.PluginConfig;
import org.briarproject.api.plugins.duplex.DuplexPlugin;
import org.briarproject.api.plugins.duplex.DuplexPluginCallback;
import org.briarproject.api.plugins.duplex.DuplexPluginFactory;
import org.briarproject.api.plugins.simplex.SimplexPlugin;
import org.briarproject.api.plugins.simplex.SimplexPluginCallback;
import org.briarproject.api.plugins.simplex.SimplexPluginFactory;
import org.briarproject.api.properties.TransportPropertyManager;
import org.briarproject.api.settings.SettingsManager;
import org.briarproject.api.ui.UiCallback;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PluginManagerImplTest extends BriarTestCase {

	@Test
	public void testStartAndStop() throws Exception {
		Mockery context = new Mockery() {{
			setThreadingPolicy(new Synchroniser());
		}};
		final Executor ioExecutor = Executors.newSingleThreadExecutor();
		final EventBus eventBus = context.mock(EventBus.class);
		final PluginConfig pluginConfig = context.mock(PluginConfig.class);
		final ConnectionManager connectionManager =
				context.mock(ConnectionManager.class);
		final SettingsManager settingsManager =
				context.mock(SettingsManager.class);
		final TransportPropertyManager transportPropertyManager =
				context.mock(TransportPropertyManager.class);
		final UiCallback uiCallback = context.mock(UiCallback.class);

		// Two simplex plugin factories: both create plugins, one fails to start
		final SimplexPluginFactory simplexFactory =
				context.mock(SimplexPluginFactory.class);
		final SimplexPlugin simplexPlugin = context.mock(SimplexPlugin.class);
		final TransportId simplexId = new TransportId("simplex");
		final SimplexPluginFactory simplexFailFactory =
				context.mock(SimplexPluginFactory.class, "simplexFailFactory");
		final SimplexPlugin simplexFailPlugin =
				context.mock(SimplexPlugin.class, "simplexFailPlugin");
		final TransportId simplexFailId = new TransportId("simplex1");

		// Two duplex plugin factories: one creates a plugin, the other fails
		final DuplexPluginFactory duplexFactory =
				context.mock(DuplexPluginFactory.class);
		final DuplexPlugin duplexPlugin = context.mock(DuplexPlugin.class);
		final TransportId duplexId = new TransportId("duplex");
		final DuplexPluginFactory duplexFailFactory =
				context.mock(DuplexPluginFactory.class, "duplexFailFactory");
		final TransportId duplexFailId = new TransportId("duplex1");

		context.checking(new Expectations() {{
			// start()
			// First simplex plugin
			oneOf(pluginConfig).getSimplexFactories();
			will(returnValue(Arrays.asList(simplexFactory,
					simplexFailFactory)));
			oneOf(simplexFactory).getId();
			will(returnValue(simplexId));
			oneOf(simplexFactory).createPlugin(with(any(
					SimplexPluginCallback.class)));
			will(returnValue(simplexPlugin)); // Created
			oneOf(simplexPlugin).start();
			will(returnValue(true)); // Started
			// Second simplex plugin
			oneOf(simplexFailFactory).getId();
			will(returnValue(simplexFailId));
			oneOf(simplexFailFactory).createPlugin(with(any(
					SimplexPluginCallback.class)));
			will(returnValue(simplexFailPlugin)); // Created
			oneOf(simplexFailPlugin).start();
			will(returnValue(false)); // Failed to start
			// First duplex plugin
			oneOf(pluginConfig).getDuplexFactories();
			will(returnValue(Arrays.asList(duplexFactory, duplexFailFactory)));
			oneOf(duplexFactory).getId();
			will(returnValue(duplexId));
			oneOf(duplexFactory).createPlugin(with(any(
					DuplexPluginCallback.class)));
			will(returnValue(duplexPlugin)); // Created
			oneOf(duplexPlugin).start();
			will(returnValue(true)); // Started
			// Second duplex plugin
			oneOf(duplexFailFactory).getId();
			will(returnValue(duplexFailId));
			oneOf(duplexFailFactory).createPlugin(with(any(
					DuplexPluginCallback.class)));
			will(returnValue(null)); // Failed to create a plugin
			// stop()
			// Stop the plugins
			oneOf(simplexPlugin).stop();
			oneOf(simplexFailPlugin).stop();
			oneOf(duplexPlugin).stop();
		}});

		PluginManagerImpl p = new PluginManagerImpl(ioExecutor, eventBus,
				pluginConfig, connectionManager, settingsManager,
				transportPropertyManager, uiCallback);

		// Two plugins should be started and stopped
		p.startService();
		p.stopService();

		context.assertIsSatisfied();
	}
}

package org.briarproject.bramble.test;

import org.briarproject.bramble.api.Pair;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.plugin.PluginCallback;
import org.briarproject.bramble.api.plugin.PluginConfig;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.plugin.duplex.DuplexPlugin;
import org.briarproject.bramble.api.plugin.duplex.DuplexPluginFactory;
import org.briarproject.bramble.api.plugin.simplex.SimplexPlugin;
import org.briarproject.bramble.api.plugin.simplex.SimplexPluginFactory;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import dagger.Module;
import dagger.Provides;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.briarproject.bramble.test.TestUtils.getTransportId;

@Module
public class TestPluginConfigModule {

	public static final TransportId SIMPLEX_TRANSPORT_ID = getTransportId();
	public static final TransportId DUPLEX_TRANSPORT_ID = getTransportId();
	public static final int MAX_LATENCY = 30_000; // 30 seconds

	@NotNullByDefault
	private final SimplexPluginFactory simplex = new SimplexPluginFactory() {

		@Override
		public TransportId getId() {
			return SIMPLEX_TRANSPORT_ID;
		}

		@Override
		public int getMaxLatency() {
			return MAX_LATENCY;
		}

		@Override
		@Nullable
		public SimplexPlugin createPlugin(PluginCallback callback) {
			return null;
		}
	};

	@NotNullByDefault
	private final DuplexPluginFactory duplex = new DuplexPluginFactory() {

		@Override
		public TransportId getId() {
			return DUPLEX_TRANSPORT_ID;
		}

		@Override
		public int getMaxLatency() {
			return MAX_LATENCY;
		}

		@Nullable
		@Override
		public DuplexPlugin createPlugin(PluginCallback callback) {
			return null;
		}
	};

	@Provides
	PluginConfig providePluginConfig() {
		@NotNullByDefault
		PluginConfig pluginConfig = new PluginConfig() {

			@Override
			public Collection<DuplexPluginFactory> getDuplexFactories() {
				return singletonList(duplex);
			}

			@Override
			public Collection<SimplexPluginFactory> getSimplexFactories() {
				return singletonList(simplex);
			}

			@Override
			public boolean shouldPoll() {
				return false;
			}


			@Override
			public List<Pair<TransportId, TransportId>> getTransportPreferences() {
				return emptyList();
			}
		};
		return pluginConfig;
	}
}

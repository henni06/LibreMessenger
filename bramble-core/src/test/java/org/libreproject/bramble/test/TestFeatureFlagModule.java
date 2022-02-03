package org.libreproject.bramble.test;

import org.libreproject.bramble.api.FeatureFlags;

import dagger.Module;
import dagger.Provides;

@Module
public class TestFeatureFlagModule {
	@Provides
	FeatureFlags provideFeatureFlags() {
		return new FeatureFlags() {
			@Override
			public boolean shouldEnableImageAttachments() {
				return true;
			}

			@Override
			public boolean shouldEnableProfilePictures() {
				return true;
			}

			@Override
			public boolean shouldEnableDisappearingMessages() {
				return true;
			}
		};
	}
}

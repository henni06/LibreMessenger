package org.libreproject.libre.android.conversation;

import org.libreproject.libre.android.activity.ActivityScope;
import org.libreproject.libre.android.conversation.glide.BriarDataFetcherFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class ConversationModule {

	@ActivityScope
	@Provides
	BriarDataFetcherFactory provideBriarDataFetcherFactory(
			BriarDataFetcherFactory dataFetcherFactory) {
		return dataFetcherFactory;
	}

}

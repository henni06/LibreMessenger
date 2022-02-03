package org.libreproject.libre.headless.forums

import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class HeadlessForumModule {

    @Provides
    @Singleton
    internal fun provideForumController(forumController: ForumControllerImpl): ForumController {
        return forumController
    }

}

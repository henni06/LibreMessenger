package org.libreproject.libre.headless.contact

import dagger.Module
import dagger.Provides
import org.libreproject.bramble.api.event.EventBus
import javax.inject.Singleton

@Module
class HeadlessContactModule {

    @Provides
    @Singleton
    internal fun provideContactController(
        eventBus: EventBus,
        contactController: ContactControllerImpl
    ): ContactController {
        eventBus.addListener(contactController)
        return contactController
    }

}

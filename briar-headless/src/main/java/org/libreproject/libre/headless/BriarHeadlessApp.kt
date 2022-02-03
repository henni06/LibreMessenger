package org.libreproject.libre.headless

import dagger.Component
import org.libreproject.bramble.BrambleCoreEagerSingletons
import org.libreproject.bramble.BrambleCoreModule
import org.libreproject.libre.BriarCoreEagerSingletons
import org.libreproject.libre.BriarCoreModule
import java.security.SecureRandom
import javax.inject.Singleton

@Component(
    modules = [
        BrambleCoreModule::class,
        BriarCoreModule::class,
        HeadlessModule::class
    ]
)
@Singleton
internal interface BriarHeadlessApp : BrambleCoreEagerSingletons, BriarCoreEagerSingletons,
    HeadlessEagerSingletons {

    fun getRouter(): Router

    fun getSecureRandom(): SecureRandom
}

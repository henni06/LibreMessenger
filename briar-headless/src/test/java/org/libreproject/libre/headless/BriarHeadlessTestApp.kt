package org.libreproject.libre.headless

import dagger.Component
import org.libreproject.bramble.BrambleCoreEagerSingletons
import org.libreproject.bramble.BrambleCoreModule
import org.libreproject.bramble.api.crypto.CryptoComponent
import org.libreproject.libre.BriarCoreEagerSingletons
import org.libreproject.libre.BriarCoreModule
import org.libreproject.libre.api.test.TestDataCreator
import javax.inject.Singleton

@Component(
    modules = [
        BrambleCoreModule::class,
        BriarCoreModule::class,
        HeadlessTestModule::class
    ]
)
@Singleton
internal interface BriarHeadlessTestApp : BrambleCoreEagerSingletons, BriarCoreEagerSingletons,
    HeadlessEagerSingletons {

    fun getRouter(): Router

    fun getCryptoComponent(): CryptoComponent

    fun getTestDataCreator(): TestDataCreator
}

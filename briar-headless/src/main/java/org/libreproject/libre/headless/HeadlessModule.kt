package org.libreproject.libre.headless

import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import org.libreproject.bramble.account.AccountModule
import org.libreproject.bramble.api.FeatureFlags
import org.libreproject.bramble.api.db.DatabaseConfig
import org.libreproject.bramble.api.plugin.PluginConfig
import org.libreproject.bramble.api.plugin.TorConstants.DEFAULT_CONTROL_PORT
import org.libreproject.bramble.api.plugin.TorConstants.DEFAULT_SOCKS_PORT
import org.libreproject.bramble.api.plugin.TorControlPort
import org.libreproject.bramble.api.plugin.TorDirectory
import org.libreproject.bramble.api.plugin.TorSocksPort
import org.libreproject.bramble.api.plugin.TransportId
import org.libreproject.bramble.api.plugin.duplex.DuplexPluginFactory
import org.libreproject.bramble.api.plugin.simplex.SimplexPluginFactory
import org.libreproject.bramble.battery.DefaultBatteryManagerModule
import org.libreproject.bramble.event.DefaultEventExecutorModule
import org.libreproject.bramble.network.JavaNetworkModule
import org.libreproject.bramble.plugin.tor.CircumventionModule
import org.libreproject.bramble.plugin.tor.UnixTorPluginFactory
import org.libreproject.bramble.socks.SocksModule
import org.libreproject.bramble.system.ClockModule
import org.libreproject.bramble.system.DefaultTaskSchedulerModule
import org.libreproject.bramble.system.DefaultWakefulIoExecutorModule
import org.libreproject.bramble.system.DesktopSecureRandomModule
import org.libreproject.bramble.system.JavaSystemModule
import org.libreproject.bramble.util.OsUtils.isLinux
import org.libreproject.bramble.util.OsUtils.isMac
import org.libreproject.libre.headless.blogs.HeadlessBlogModule
import org.libreproject.libre.headless.contact.HeadlessContactModule
import org.libreproject.libre.headless.event.HeadlessEventModule
import org.libreproject.libre.headless.forums.HeadlessForumModule
import org.libreproject.libre.headless.messaging.HeadlessMessagingModule
import java.io.File
import java.util.Collections.emptyList
import javax.inject.Singleton

@Module(
    includes = [
        AccountModule::class,
        CircumventionModule::class,
        ClockModule::class,
        DefaultBatteryManagerModule::class,
        DefaultEventExecutorModule::class,
        DefaultTaskSchedulerModule::class,
        DefaultWakefulIoExecutorModule::class,
        DesktopSecureRandomModule::class,
        HeadlessBlogModule::class,
        HeadlessContactModule::class,
        HeadlessEventModule::class,
        HeadlessForumModule::class,
        HeadlessMessagingModule::class,
        JavaNetworkModule::class,
        JavaSystemModule::class,
        SocksModule::class
    ]
)
internal class HeadlessModule(private val appDir: File) {

    @Provides
    @Singleton
    internal fun provideBriarService(briarService: BriarServiceImpl): BriarService = briarService

    @Provides
    @Singleton
    internal fun provideDatabaseConfig(): DatabaseConfig {
        val dbDir = File(appDir, "db")
        val keyDir = File(appDir, "key")
        return HeadlessDatabaseConfig(dbDir, keyDir)
    }

    @Provides
    @TorDirectory
    internal fun provideTorDirectory(): File {
        return File(appDir, "tor")
    }

    @Provides
    @TorSocksPort
    internal fun provideTorSocksPort(): Int = DEFAULT_SOCKS_PORT

    @Provides
    @TorControlPort
    internal fun provideTorControlPort(): Int = DEFAULT_CONTROL_PORT

    @Provides
    @Singleton
    internal fun providePluginConfig(tor: UnixTorPluginFactory): PluginConfig {
        val duplex: List<DuplexPluginFactory> =
            if (isLinux() || isMac()) listOf(tor) else emptyList()
        return object : PluginConfig {
            override fun getDuplexFactories(): Collection<DuplexPluginFactory> = duplex
            override fun getSimplexFactories(): Collection<SimplexPluginFactory> = emptyList()
            override fun shouldPoll(): Boolean = true
            override fun getTransportPreferences(): Map<TransportId, List<TransportId>> = emptyMap()
        }
    }

    @Provides
    @Singleton
    internal fun provideObjectMapper() = ObjectMapper()

    @Provides
    internal fun provideFeatureFlags() = object : FeatureFlags {
        override fun shouldEnableImageAttachments() = false
        override fun shouldEnableProfilePictures() = false
        override fun shouldEnableDisappearingMessages() = false
    }
}

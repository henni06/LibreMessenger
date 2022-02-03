package org.libreproject.bramble;

import org.libreproject.bramble.cleanup.CleanupModule;
import org.libreproject.bramble.client.ClientModule;
import org.libreproject.bramble.connection.ConnectionModule;
import org.libreproject.bramble.contact.ContactModule;
import org.libreproject.bramble.crypto.CryptoExecutorModule;
import org.libreproject.bramble.crypto.CryptoModule;
import org.libreproject.bramble.data.DataModule;
import org.libreproject.bramble.db.DatabaseExecutorModule;
import org.libreproject.bramble.db.DatabaseModule;
import org.libreproject.bramble.event.EventModule;
import org.libreproject.bramble.identity.IdentityModule;
import org.libreproject.bramble.io.IoModule;
import org.libreproject.bramble.keyagreement.KeyAgreementModule;
import org.libreproject.bramble.lifecycle.LifecycleModule;
import org.libreproject.bramble.mailbox.MailboxModule;
import org.libreproject.bramble.plugin.PluginModule;
import org.libreproject.bramble.properties.PropertiesModule;
import org.libreproject.bramble.record.RecordModule;
import org.libreproject.bramble.reliability.ReliabilityModule;
import org.libreproject.bramble.rendezvous.RendezvousModule;
import org.libreproject.bramble.settings.SettingsModule;
import org.libreproject.bramble.sync.SyncModule;
import org.libreproject.bramble.sync.validation.ValidationModule;
import org.libreproject.bramble.transport.TransportModule;
import org.libreproject.bramble.transport.agreement.TransportKeyAgreementModule;
import org.libreproject.bramble.versioning.VersioningModule;

import dagger.Module;

@Module(includes = {
		CleanupModule.class,
		ClientModule.class,
		ConnectionModule.class,
		ContactModule.class,
		CryptoModule.class,
		CryptoExecutorModule.class,
		DataModule.class,
		DatabaseModule.class,
		DatabaseExecutorModule.class,
		EventModule.class,
		IdentityModule.class,
		IoModule.class,
		KeyAgreementModule.class,
		LifecycleModule.class,
		MailboxModule.class,
		PluginModule.class,
		PropertiesModule.class,
		RecordModule.class,
		ReliabilityModule.class,
		RendezvousModule.class,
		SettingsModule.class,
		SyncModule.class,
		TransportKeyAgreementModule.class,
		TransportModule.class,
		ValidationModule.class,
		VersioningModule.class
})
public class BrambleCoreModule {
}

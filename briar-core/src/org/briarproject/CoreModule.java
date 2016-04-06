package org.briarproject;

import org.briarproject.clients.ClientsModule;
import org.briarproject.contact.ContactModule;
import org.briarproject.crypto.CryptoModule;
import org.briarproject.data.DataModule;
import org.briarproject.db.DatabaseExecutorModule;
import org.briarproject.db.DatabaseModule;
import org.briarproject.event.EventModule;
import org.briarproject.forum.ForumModule;
import org.briarproject.identity.IdentityModule;
import org.briarproject.introduction.IntroductionModule;
import org.briarproject.invitation.InvitationModule;
import org.briarproject.keyagreement.KeyAgreementModule;
import org.briarproject.lifecycle.LifecycleModule;
import org.briarproject.messaging.MessagingModule;
import org.briarproject.plugins.PluginsModule;
import org.briarproject.properties.PropertiesModule;
import org.briarproject.reliability.ReliabilityModule;
import org.briarproject.settings.SettingsModule;
import org.briarproject.sync.SyncModule;
import org.briarproject.system.SystemModule;
import org.briarproject.transport.TransportModule;

import dagger.Module;

@Module(includes = {
		ClientsModule.class,
		ContactModule.class,
		CryptoModule.class,
		DataModule.class,
		DatabaseModule.class,
		DatabaseExecutorModule.class,
		EventModule.class,
		ForumModule.class,
		IdentityModule.class,
		IntroductionModule.class,
		InvitationModule.class,
		KeyAgreementModule.class,
		LifecycleModule.class,
		MessagingModule.class,
		PluginsModule.class,
		PropertiesModule.class,
		ReliabilityModule.class,
		SettingsModule.class,
		SyncModule.class,
		SystemModule.class,
		TransportModule.class
})
public class CoreModule {

	public static void initEagerSingletons(CoreEagerSingletons c) {
		c.inject(new ContactModule.EagerSingletons());
		c.inject(new CryptoModule.EagerSingletons());
		c.inject(new DatabaseExecutorModule.EagerSingletons());
		c.inject(new ForumModule.EagerSingletons());
		c.inject(new LifecycleModule.EagerSingletons());
		c.inject(new MessagingModule.EagerSingletons());
		c.inject(new PluginsModule.EagerSingletons());
		c.inject(new PropertiesModule.EagerSingletons());
		c.inject(new SyncModule.EagerSingletons());
		c.inject(new TransportModule.EagerSingletons());
		c.inject(new IntroductionModule.EagerSingletons());
	}
}

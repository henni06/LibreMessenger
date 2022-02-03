package org.libreproject.bramble;

import org.libreproject.bramble.cleanup.CleanupModule;
import org.libreproject.bramble.contact.ContactModule;
import org.libreproject.bramble.crypto.CryptoExecutorModule;
import org.libreproject.bramble.db.DatabaseExecutorModule;
import org.libreproject.bramble.identity.IdentityModule;
import org.libreproject.bramble.lifecycle.LifecycleModule;
import org.libreproject.bramble.plugin.PluginModule;
import org.libreproject.bramble.properties.PropertiesModule;
import org.libreproject.bramble.rendezvous.RendezvousModule;
import org.libreproject.bramble.sync.validation.ValidationModule;
import org.libreproject.bramble.transport.TransportModule;
import org.libreproject.bramble.transport.agreement.TransportKeyAgreementModule;
import org.libreproject.bramble.versioning.VersioningModule;

public interface BrambleCoreEagerSingletons {

	void inject(CleanupModule.EagerSingletons init);

	void inject(ContactModule.EagerSingletons init);

	void inject(CryptoExecutorModule.EagerSingletons init);

	void inject(DatabaseExecutorModule.EagerSingletons init);

	void inject(IdentityModule.EagerSingletons init);

	void inject(LifecycleModule.EagerSingletons init);

	void inject(PluginModule.EagerSingletons init);

	void inject(PropertiesModule.EagerSingletons init);

	void inject(RendezvousModule.EagerSingletons init);

	void inject(TransportKeyAgreementModule.EagerSingletons init);

	void inject(TransportModule.EagerSingletons init);

	void inject(ValidationModule.EagerSingletons init);

	void inject(VersioningModule.EagerSingletons init);

	class Helper {

		public static void injectEagerSingletons(BrambleCoreEagerSingletons c) {
			c.inject(new CleanupModule.EagerSingletons());
			c.inject(new ContactModule.EagerSingletons());
			c.inject(new CryptoExecutorModule.EagerSingletons());
			c.inject(new DatabaseExecutorModule.EagerSingletons());
			c.inject(new IdentityModule.EagerSingletons());
			c.inject(new LifecycleModule.EagerSingletons());
			c.inject(new RendezvousModule.EagerSingletons());
			c.inject(new PluginModule.EagerSingletons());
			c.inject(new PropertiesModule.EagerSingletons());
			c.inject(new TransportKeyAgreementModule.EagerSingletons());
			c.inject(new TransportModule.EagerSingletons());
			c.inject(new ValidationModule.EagerSingletons());
			c.inject(new VersioningModule.EagerSingletons());
		}
	}
}

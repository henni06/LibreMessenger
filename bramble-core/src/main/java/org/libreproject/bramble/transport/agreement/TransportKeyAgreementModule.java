package org.libreproject.bramble.transport.agreement;

import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.sync.validation.ValidationManager;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.bramble.api.transport.agreement.TransportKeyAgreementManager;
import org.libreproject.bramble.api.versioning.ClientVersioningManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.libreproject.bramble.api.transport.agreement.TransportKeyAgreementManager.CLIENT_ID;
import static org.libreproject.bramble.api.transport.agreement.TransportKeyAgreementManager.MAJOR_VERSION;
import static org.libreproject.bramble.api.transport.agreement.TransportKeyAgreementManager.MINOR_VERSION;

@Module
public class TransportKeyAgreementModule {

	public static class EagerSingletons {
		@Inject
		TransportKeyAgreementManager transportKeyAgreementManager;
		@Inject
		TransportKeyAgreementValidator transportKeyAgreementValidator;
	}

	@Provides
	@Singleton
	TransportKeyAgreementManager provideTransportKeyAgreementManager(
			LifecycleManager lifecycleManager,
			ValidationManager validationManager,
			ContactManager contactManager,
			ClientVersioningManager clientVersioningManager,
			TransportKeyAgreementManagerImpl transportKeyAgreementManager) {
		lifecycleManager.registerOpenDatabaseHook(transportKeyAgreementManager);
		validationManager.registerIncomingMessageHook(CLIENT_ID,
				MAJOR_VERSION, transportKeyAgreementManager);
		contactManager.registerContactHook(transportKeyAgreementManager);
		clientVersioningManager.registerClient(CLIENT_ID, MAJOR_VERSION,
				MINOR_VERSION, transportKeyAgreementManager);
		return transportKeyAgreementManager;
	}

	@Provides
	@Singleton
	TransportKeyAgreementValidator provideTransportKeyAgreementValidator(
			ClientHelper clientHelper, MetadataEncoder metadataEncoder,
			Clock clock, MessageEncoder messageEncoder,
			ValidationManager validationManager) {
		TransportKeyAgreementValidator validator =
				new TransportKeyAgreementValidator(clientHelper,
						metadataEncoder, clock, messageEncoder);
		validationManager.registerMessageValidator(CLIENT_ID, MAJOR_VERSION,
				validator);
		return validator;
	}

	@Provides
	MessageEncoder provideMessageEncoder(MessageEncoderImpl messageEncoder) {
		return messageEncoder;
	}

	@Provides
	SessionEncoder provideSessionEncoder(SessionEncoderImpl sessionEncoder) {
		return sessionEncoder;
	}

	@Provides
	SessionParser provideSessionParser(SessionParserImpl sessionParser) {
		return sessionParser;
	}

	@Provides
	TransportKeyAgreementCrypto provideTransportKeyAgreementCrypto(
			TransportKeyAgreementCryptoImpl transportKeyAgreementCrypto) {
		return transportKeyAgreementCrypto;
	}
}

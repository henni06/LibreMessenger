package org.libreproject.bramble.keyagreement;

import org.libreproject.bramble.api.data.BdfReaderFactory;
import org.libreproject.bramble.api.data.BdfWriterFactory;
import org.libreproject.bramble.api.keyagreement.KeyAgreementTask;
import org.libreproject.bramble.api.keyagreement.PayloadEncoder;
import org.libreproject.bramble.api.keyagreement.PayloadParser;

import dagger.Module;
import dagger.Provides;

@Module
public class KeyAgreementModule {

	@Provides
	KeyAgreementTask provideKeyAgreementTask(
			KeyAgreementTaskImpl keyAgreementTask) {
		return keyAgreementTask;
	}

	@Provides
	PayloadEncoder providePayloadEncoder(BdfWriterFactory bdfWriterFactory) {
		return new PayloadEncoderImpl(bdfWriterFactory);
	}

	@Provides
	PayloadParser providePayloadParser(BdfReaderFactory bdfReaderFactory) {
		return new PayloadParserImpl(bdfReaderFactory);
	}

	@Provides
	ConnectionChooser provideConnectionChooser(
			ConnectionChooserImpl connectionChooser) {
		return connectionChooser;
	}
}

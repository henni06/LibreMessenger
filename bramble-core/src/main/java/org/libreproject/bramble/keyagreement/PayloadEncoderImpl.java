package org.libreproject.bramble.keyagreement;

import org.libreproject.bramble.api.data.BdfWriter;
import org.libreproject.bramble.api.data.BdfWriterFactory;
import org.libreproject.bramble.api.keyagreement.Payload;
import org.libreproject.bramble.api.keyagreement.PayloadEncoder;
import org.libreproject.bramble.api.keyagreement.TransportDescriptor;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.libreproject.bramble.api.keyagreement.KeyAgreementConstants.PROTOCOL_VERSION;

@Immutable
@NotNullByDefault
class PayloadEncoderImpl implements PayloadEncoder {

	private final BdfWriterFactory bdfWriterFactory;

	@Inject
	PayloadEncoderImpl(BdfWriterFactory bdfWriterFactory) {
		this.bdfWriterFactory = bdfWriterFactory;
	}

	@Override
	public byte[] encode(Payload p) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(PROTOCOL_VERSION);
		BdfWriter w = bdfWriterFactory.createWriter(out);
		try {
			w.writeListStart(); // Payload start
			w.writeRaw(p.getCommitment());
			for (TransportDescriptor d : p.getTransportDescriptors())
				w.writeList(d.getDescriptor());
			w.writeListEnd(); // Payload end
		} catch (IOException e) {
			// Shouldn't happen with ByteArrayOutputStream
			throw new AssertionError(e);
		}
		return out.toByteArray();
	}
}

package org.briarproject.keyagreement;

import org.briarproject.api.FormatException;
import org.briarproject.api.TransportId;
import org.briarproject.api.data.BdfList;
import org.briarproject.api.data.BdfReader;
import org.briarproject.api.data.BdfReaderFactory;
import org.briarproject.api.keyagreement.Payload;
import org.briarproject.api.keyagreement.PayloadParser;
import org.briarproject.api.nullsafety.NotNullByDefault;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.briarproject.api.keyagreement.KeyAgreementConstants.COMMIT_LENGTH;
import static org.briarproject.api.keyagreement.KeyAgreementConstants.PROTOCOL_VERSION;
import static org.briarproject.api.keyagreement.KeyAgreementConstants.TRANSPORT_ID_BLUETOOTH;
import static org.briarproject.api.keyagreement.KeyAgreementConstants.TRANSPORT_ID_LAN;

@Immutable
@NotNullByDefault
class PayloadParserImpl implements PayloadParser {

	private final BdfReaderFactory bdfReaderFactory;

	@Inject
	PayloadParserImpl(BdfReaderFactory bdfReaderFactory) {
		this.bdfReaderFactory = bdfReaderFactory;
	}

	@Override
	public Payload parse(byte[] raw) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(raw);
		BdfReader r = bdfReaderFactory.createReader(in);
		// The payload is a BDF list with two or more elements
		BdfList payload = r.readList();
		if (payload.size() < 2) throw new FormatException();
		if (!r.eof()) throw new FormatException();
		// First element: the protocol version
		long protocolVersion = payload.getLong(0);
		if (protocolVersion != PROTOCOL_VERSION) throw new FormatException();
		// Second element: the public key commitment
		byte[] commitment = payload.getRaw(1);
		if (commitment.length != COMMIT_LENGTH) throw new FormatException();
		// Remaining elements: transport descriptors
		Map<TransportId, BdfList> recognised =
				new HashMap<TransportId, BdfList>();
		for (int i = 2; i < payload.size(); i++) {
			BdfList descriptor = payload.getList(i);
			long transportId = descriptor.getLong(0);
			if (transportId == TRANSPORT_ID_BLUETOOTH) {
				recognised.put(new TransportId("bt"), descriptor);
			} else if (transportId == TRANSPORT_ID_LAN) {
				recognised.put(new TransportId("lan"), descriptor);
			}
		}
		return new Payload(commitment, recognised);
	}
}

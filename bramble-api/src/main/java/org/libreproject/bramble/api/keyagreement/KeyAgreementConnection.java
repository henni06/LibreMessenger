package org.libreproject.bramble.api.keyagreement;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.plugin.duplex.DuplexTransportConnection;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class KeyAgreementConnection {

	private final DuplexTransportConnection conn;
	private final TransportId id;

	public KeyAgreementConnection(DuplexTransportConnection conn,
			TransportId id) {
		this.conn = conn;
		this.id = id;
	}

	public DuplexTransportConnection getConnection() {
		return conn;
	}

	public TransportId getTransportId() {
		return id;
	}
}

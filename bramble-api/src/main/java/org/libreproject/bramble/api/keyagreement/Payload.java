package org.libreproject.bramble.api.keyagreement;

import org.libreproject.bramble.api.Bytes;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.util.List;

import javax.annotation.concurrent.Immutable;

/**
 * A BQP payload.
 */
@Immutable
@NotNullByDefault
public class Payload implements Comparable<Payload> {

	private final Bytes commitment;
	private final List<TransportDescriptor> descriptors;

	public Payload(byte[] commitment, List<TransportDescriptor> descriptors) {
		this.commitment = new Bytes(commitment);
		this.descriptors = descriptors;
	}

	/**
	 * Returns the commitment contained in this payload.
	 */
	public byte[] getCommitment() {
		return commitment.getBytes();
	}

	/**
	 * Returns the transport descriptors contained in this payload.
	 */
	public List<TransportDescriptor> getTransportDescriptors() {
		return descriptors;
	}

	@Override
	public int compareTo(Payload p) {
		return commitment.compareTo(p.commitment);
	}
}

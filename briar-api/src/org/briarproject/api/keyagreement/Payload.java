package org.briarproject.api.keyagreement;

import org.briarproject.api.Bytes;

import java.util.List;

/**
 * A BQP payload.
 */
public class Payload implements Comparable<Payload> {

	private final Bytes commitment;
	private final List<TransportDescriptor> descriptors;

	public Payload(byte[] commitment, List<TransportDescriptor> descriptors) {
		this.commitment = new Bytes(commitment);
		this.descriptors = descriptors;
	}

	/** Returns the commitment contained in this payload. */
	public byte[] getCommitment() {
		return commitment.getBytes();
	}

	/** Returns the transport descriptors contained in this payload. */
	public List<TransportDescriptor> getTransportDescriptors() {
		return descriptors;
	}

	@Override
	public int compareTo(Payload p) {
		return commitment.compareTo(p.commitment);
	}
}

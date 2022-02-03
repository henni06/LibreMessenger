package org.libreproject.bramble.api.keyagreement;

import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportId;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class TransportDescriptor {

	private final TransportId id;
	private final BdfList descriptor;

	public TransportDescriptor(TransportId id, BdfList descriptor) {
		this.id = id;
		this.descriptor = descriptor;
	}

	public TransportId getId() {
		return id;
	}

	public BdfList getDescriptor() {
		return descriptor;
	}
}

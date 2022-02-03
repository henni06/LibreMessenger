package org.libreproject.bramble.api.keyagreement;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.IOException;

@NotNullByDefault
public interface PayloadParser {

	Payload parse(byte[] raw) throws IOException;
}

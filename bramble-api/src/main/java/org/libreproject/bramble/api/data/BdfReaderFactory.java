package org.libreproject.bramble.api.data;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.InputStream;

@NotNullByDefault
public interface BdfReaderFactory {

	BdfReader createReader(InputStream in);

	BdfReader createReader(InputStream in, int nestedLimit,
			int maxBufferSize);
}

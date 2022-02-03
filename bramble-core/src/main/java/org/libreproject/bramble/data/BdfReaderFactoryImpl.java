package org.libreproject.bramble.data;

import org.libreproject.bramble.api.data.BdfReader;
import org.libreproject.bramble.api.data.BdfReaderFactory;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.InputStream;

import javax.annotation.concurrent.Immutable;

import static org.libreproject.bramble.api.data.BdfReader.DEFAULT_MAX_BUFFER_SIZE;
import static org.libreproject.bramble.api.data.BdfReader.DEFAULT_NESTED_LIMIT;

@Immutable
@NotNullByDefault
public
class BdfReaderFactoryImpl implements BdfReaderFactory {

	@Override
	public BdfReader createReader(InputStream in) {
		return new BdfReaderImpl(in, DEFAULT_NESTED_LIMIT,
				DEFAULT_MAX_BUFFER_SIZE);
	}

	@Override
	public BdfReader createReader(InputStream in, int nestedLimit,
			int maxBufferSize) {
		return new BdfReaderImpl(in, nestedLimit, maxBufferSize);
	}
}

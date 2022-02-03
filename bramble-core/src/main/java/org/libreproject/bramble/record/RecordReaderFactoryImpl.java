package org.libreproject.bramble.record;

import org.libreproject.bramble.api.record.RecordReader;
import org.libreproject.bramble.api.record.RecordReaderFactory;

import java.io.InputStream;

class RecordReaderFactoryImpl implements RecordReaderFactory {

	@Override
	public RecordReader createRecordReader(InputStream in) {
		return new RecordReaderImpl(in);
	}
}

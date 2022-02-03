package org.libreproject.bramble.record;

import org.libreproject.bramble.api.record.RecordWriter;
import org.libreproject.bramble.api.record.RecordWriterFactory;

import java.io.OutputStream;

class RecordWriterFactoryImpl implements RecordWriterFactory {

	@Override
	public RecordWriter createRecordWriter(OutputStream out) {
		return new RecordWriterImpl(out);
	}
}

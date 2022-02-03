package org.libreproject.bramble.api.record;

import java.io.OutputStream;

public interface RecordWriterFactory {

	RecordWriter createRecordWriter(OutputStream out);
}

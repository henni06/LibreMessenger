package org.libreproject.bramble.api.record;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.IOException;

@NotNullByDefault
public interface RecordWriter {

	void writeRecord(Record r) throws IOException;

	void flush() throws IOException;

	void close() throws IOException;
}

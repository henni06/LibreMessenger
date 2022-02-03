package org.libreproject.bramble.api.sync;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.OutputStream;

@NotNullByDefault
public interface SyncRecordWriterFactory {

	SyncRecordWriter createRecordWriter(OutputStream out);
}

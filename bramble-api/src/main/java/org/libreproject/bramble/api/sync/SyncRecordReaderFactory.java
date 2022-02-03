package org.libreproject.bramble.api.sync;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.InputStream;

@NotNullByDefault
public interface SyncRecordReaderFactory {

	SyncRecordReader createRecordReader(InputStream in);
}

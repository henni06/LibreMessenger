package org.libreproject.bramble.sync;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.record.RecordWriter;
import org.libreproject.bramble.api.record.RecordWriterFactory;
import org.libreproject.bramble.api.sync.MessageFactory;
import org.libreproject.bramble.api.sync.SyncRecordWriter;
import org.libreproject.bramble.api.sync.SyncRecordWriterFactory;

import java.io.OutputStream;

import javax.inject.Inject;

@NotNullByDefault
class SyncRecordWriterFactoryImpl implements SyncRecordWriterFactory {

	private final MessageFactory messageFactory;
	private final RecordWriterFactory recordWriterFactory;

	@Inject
	SyncRecordWriterFactoryImpl(MessageFactory messageFactory,
			RecordWriterFactory recordWriterFactory) {
		this.messageFactory = messageFactory;
		this.recordWriterFactory = recordWriterFactory;
	}

	@Override
	public SyncRecordWriter createRecordWriter(OutputStream out) {
		RecordWriter writer = recordWriterFactory.createRecordWriter(out);
		return new SyncRecordWriterImpl(messageFactory, writer);
	}
}

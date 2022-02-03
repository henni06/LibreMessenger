package org.libreproject.bramble.sync;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.record.Record;
import org.libreproject.bramble.api.record.RecordWriter;
import org.libreproject.bramble.api.sync.Ack;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageFactory;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.api.sync.Offer;
import org.libreproject.bramble.api.sync.Priority;
import org.libreproject.bramble.api.sync.Request;
import org.libreproject.bramble.api.sync.SyncRecordWriter;
import org.libreproject.bramble.api.sync.Versions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.annotation.concurrent.NotThreadSafe;

import static org.libreproject.bramble.api.sync.RecordTypes.ACK;
import static org.libreproject.bramble.api.sync.RecordTypes.MESSAGE;
import static org.libreproject.bramble.api.sync.RecordTypes.OFFER;
import static org.libreproject.bramble.api.sync.RecordTypes.PRIORITY;
import static org.libreproject.bramble.api.sync.RecordTypes.REQUEST;
import static org.libreproject.bramble.api.sync.RecordTypes.VERSIONS;
import static org.libreproject.bramble.api.sync.SyncConstants.PROTOCOL_VERSION;

@NotThreadSafe
@NotNullByDefault
class SyncRecordWriterImpl implements SyncRecordWriter {

	private final MessageFactory messageFactory;
	private final RecordWriter writer;
	private final ByteArrayOutputStream payload = new ByteArrayOutputStream();

	SyncRecordWriterImpl(MessageFactory messageFactory, RecordWriter writer) {
		this.messageFactory = messageFactory;
		this.writer = writer;
	}

	private void writeRecord(byte recordType) throws IOException {
		writer.writeRecord(new Record(PROTOCOL_VERSION, recordType,
				payload.toByteArray()));
		payload.reset();
	}

	@Override
	public void writeAck(Ack a) throws IOException {
		for (MessageId m : a.getMessageIds()) payload.write(m.getBytes());
		writeRecord(ACK);
	}

	@Override
	public void writeMessage(Message m) throws IOException {
		byte[] raw = messageFactory.getRawMessage(m);
		writer.writeRecord(new Record(PROTOCOL_VERSION, MESSAGE, raw));
	}

	@Override
	public void writeOffer(Offer o) throws IOException {
		for (MessageId m : o.getMessageIds()) payload.write(m.getBytes());
		writeRecord(OFFER);
	}

	@Override
	public void writeRequest(Request r) throws IOException {
		for (MessageId m : r.getMessageIds()) payload.write(m.getBytes());
		writeRecord(REQUEST);
	}

	@Override
	public void writeVersions(Versions v) throws IOException {
		for (byte b : v.getSupportedVersions()) payload.write(b);
		writeRecord(VERSIONS);
	}

	@Override
	public void writePriority(Priority p) throws IOException {
		writer.writeRecord(
				new Record(PROTOCOL_VERSION, PRIORITY, p.getNonce()));
	}

	@Override
	public void flush() throws IOException {
		writer.flush();
	}
}

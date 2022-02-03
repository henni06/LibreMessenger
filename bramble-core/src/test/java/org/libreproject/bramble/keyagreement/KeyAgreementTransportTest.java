package org.libreproject.bramble.keyagreement;

import org.libreproject.bramble.api.Predicate;
import org.libreproject.bramble.api.keyagreement.KeyAgreementConnection;
import org.libreproject.bramble.api.plugin.TransportConnectionReader;
import org.libreproject.bramble.api.plugin.TransportConnectionWriter;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.plugin.duplex.DuplexTransportConnection;
import org.libreproject.bramble.api.record.Record;
import org.libreproject.bramble.api.record.RecordReader;
import org.libreproject.bramble.api.record.RecordReaderFactory;
import org.libreproject.bramble.api.record.RecordWriter;
import org.libreproject.bramble.api.record.RecordWriterFactory;
import org.libreproject.bramble.test.BrambleMockTestCase;
import org.libreproject.bramble.test.CaptureArgumentAction;
import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import static org.libreproject.bramble.api.keyagreement.KeyAgreementConstants.PROTOCOL_VERSION;
import static org.libreproject.bramble.api.keyagreement.RecordTypes.ABORT;
import static org.libreproject.bramble.api.keyagreement.RecordTypes.CONFIRM;
import static org.libreproject.bramble.api.keyagreement.RecordTypes.KEY;
import static org.libreproject.bramble.test.TestUtils.getRandomBytes;
import static org.libreproject.bramble.test.TestUtils.getTransportId;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class KeyAgreementTransportTest extends BrambleMockTestCase {

	private final DuplexTransportConnection duplexTransportConnection =
			context.mock(DuplexTransportConnection.class);
	private final TransportConnectionReader transportConnectionReader =
			context.mock(TransportConnectionReader.class);
	private final TransportConnectionWriter transportConnectionWriter =
			context.mock(TransportConnectionWriter.class);
	private final RecordReaderFactory recordReaderFactory =
			context.mock(RecordReaderFactory.class);
	private final RecordWriterFactory recordWriterFactory =
			context.mock(RecordWriterFactory.class);
	private final RecordReader recordReader = context.mock(RecordReader.class);
	private final RecordWriter recordWriter = context.mock(RecordWriter.class);

	private final TransportId transportId = getTransportId();
	private final KeyAgreementConnection keyAgreementConnection =
			new KeyAgreementConnection(duplexTransportConnection, transportId);

	private final InputStream inputStream;
	private final OutputStream outputStream;

	private KeyAgreementTransport kat;

	public KeyAgreementTransportTest() {
		context.setImposteriser(ClassImposteriser.INSTANCE);
		inputStream = context.mock(InputStream.class);
		outputStream = context.mock(OutputStream.class);
	}

	@Test
	public void testSendKey() throws Exception {
		byte[] key = getRandomBytes(123);

		setup();
		AtomicReference<Record> written = expectWriteRecord();

		kat.sendKey(key);
		assertNotNull(written.get());
		assertRecordEquals(KEY, key, written.get());
	}

	@Test
	public void testSendConfirm() throws Exception {
		byte[] confirm = getRandomBytes(123);

		setup();
		AtomicReference<Record> written = expectWriteRecord();

		kat.sendConfirm(confirm);
		assertNotNull(written.get());
		assertRecordEquals(CONFIRM, confirm, written.get());
	}

	@Test
	public void testSendAbortWithException() throws Exception {
		setup();
		AtomicReference<Record> written = expectWriteRecord();
		context.checking(new Expectations() {{
			oneOf(transportConnectionReader).dispose(true, true);
			oneOf(transportConnectionWriter).dispose(true);
		}});

		kat.sendAbort(true);
		assertNotNull(written.get());
		assertRecordEquals(ABORT, new byte[0], written.get());
	}

	@Test
	public void testSendAbortWithoutException() throws Exception {
		setup();
		AtomicReference<Record> written = expectWriteRecord();
		context.checking(new Expectations() {{
			oneOf(transportConnectionReader).dispose(false, true);
			oneOf(transportConnectionWriter).dispose(false);
		}});

		kat.sendAbort(false);
		assertNotNull(written.get());
		assertRecordEquals(ABORT, new byte[0], written.get());
	}

	@Test(expected = AbortException.class)
	public void testReceiveKeyThrowsExceptionIfAtEndOfStream()
			throws Exception {
		setup();
		expectReadRecord(null);

		kat.receiveKey();
	}

	@Test(expected = AbortException.class)
	public void testReceiveKeyThrowsExceptionIfAbortIsReceived()
			throws Exception {
		setup();
		expectReadRecord(new Record(PROTOCOL_VERSION, ABORT, new byte[0]));

		kat.receiveKey();
	}

	@Test(expected = AbortException.class)
	public void testReceiveKeyThrowsExceptionIfConfirmIsReceived()
			throws Exception {
		byte[] confirm = getRandomBytes(123);

		setup();
		expectReadRecord(new Record(PROTOCOL_VERSION, CONFIRM, confirm));

		kat.receiveKey();
	}

	@Test(expected = AbortException.class)
	public void testReceiveConfirmThrowsExceptionIfAtEndOfStream()
			throws Exception {
		setup();
		expectReadRecord(null);

		kat.receiveConfirm();
	}

	@Test(expected = AbortException.class)
	public void testReceiveConfirmThrowsExceptionIfAbortIsReceived()
			throws Exception {
		setup();
		expectReadRecord(new Record(PROTOCOL_VERSION, ABORT, new byte[0]));

		kat.receiveConfirm();
	}

	@Test(expected = AbortException.class)
	public void testReceiveConfirmThrowsExceptionIfKeyIsReceived()
			throws Exception {
		byte[] key = getRandomBytes(123);

		setup();
		expectReadRecord(new Record(PROTOCOL_VERSION, KEY, key));

		kat.receiveConfirm();
	}

	private void setup() throws Exception {
		context.checking(new Expectations() {{
			allowing(duplexTransportConnection).getReader();
			will(returnValue(transportConnectionReader));
			allowing(transportConnectionReader).getInputStream();
			will(returnValue(inputStream));
			oneOf(recordReaderFactory).createRecordReader(inputStream);
			will(returnValue(recordReader));
			allowing(duplexTransportConnection).getWriter();
			will(returnValue(transportConnectionWriter));
			allowing(transportConnectionWriter).getOutputStream();
			will(returnValue(outputStream));
			oneOf(recordWriterFactory).createRecordWriter(outputStream);
			will(returnValue(recordWriter));
		}});
		kat = new KeyAgreementTransport(recordReaderFactory,
				recordWriterFactory, keyAgreementConnection);
	}

	private AtomicReference<Record> expectWriteRecord() throws Exception {
		AtomicReference<Record> captured = new AtomicReference<>();
		context.checking(new Expectations() {{
			oneOf(recordWriter).writeRecord(with(any(Record.class)));
			will(new CaptureArgumentAction<>(captured, Record.class, 0));
			oneOf(recordWriter).flush();
		}});
		return captured;
	}

	private void assertRecordEquals(byte expectedType,
			byte[] expectedPayload, Record actual) {
		assertEquals(PROTOCOL_VERSION, actual.getProtocolVersion());
		assertEquals(expectedType, actual.getRecordType());
		assertArrayEquals(expectedPayload, actual.getPayload());
	}

	private void expectReadRecord(@Nullable Record record) throws Exception {
		context.checking(new Expectations() {{
			//noinspection unchecked
			oneOf(recordReader).readRecord(with(any(Predicate.class)),
					with(any(Predicate.class)));
			will(returnValue(record));
		}});
	}
}

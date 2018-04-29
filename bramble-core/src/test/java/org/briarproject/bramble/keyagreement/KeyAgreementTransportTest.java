package org.briarproject.bramble.keyagreement;

import org.briarproject.bramble.api.keyagreement.KeyAgreementConnection;
import org.briarproject.bramble.api.plugin.TransportConnectionReader;
import org.briarproject.bramble.api.plugin.TransportConnectionWriter;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.plugin.duplex.DuplexTransportConnection;
import org.briarproject.bramble.api.record.Record;
import org.briarproject.bramble.api.record.RecordReader;
import org.briarproject.bramble.api.record.RecordReaderFactory;
import org.briarproject.bramble.api.record.RecordWriter;
import org.briarproject.bramble.api.record.RecordWriterFactory;
import org.briarproject.bramble.test.BrambleMockTestCase;
import org.briarproject.bramble.test.CaptureArgumentAction;
import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.briarproject.bramble.api.keyagreement.KeyAgreementConstants.PROTOCOL_VERSION;
import static org.briarproject.bramble.api.keyagreement.RecordTypes.ABORT;
import static org.briarproject.bramble.api.keyagreement.RecordTypes.CONFIRM;
import static org.briarproject.bramble.api.keyagreement.RecordTypes.KEY;
import static org.briarproject.bramble.test.TestUtils.getRandomBytes;
import static org.briarproject.bramble.test.TestUtils.getTransportId;
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
		assertRecordEquals(PROTOCOL_VERSION, KEY, key, written.get());
	}

	@Test
	public void testSendConfirm() throws Exception {
		byte[] confirm = getRandomBytes(123);

		setup();
		AtomicReference<Record> written = expectWriteRecord();

		kat.sendConfirm(confirm);
		assertNotNull(written.get());
		assertRecordEquals(PROTOCOL_VERSION, CONFIRM, confirm, written.get());
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
		assertRecordEquals(PROTOCOL_VERSION, ABORT, new byte[0], written.get());
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
		assertRecordEquals(PROTOCOL_VERSION, ABORT, new byte[0], written.get());
	}

	@Test(expected = AbortException.class)
	public void testReceiveKeyThrowsExceptionIfAtEndOfStream()
			throws Exception {
		setup();
		context.checking(new Expectations() {{
			oneOf(recordReader).readRecord();
			will(throwException(new EOFException()));
		}});

		kat.receiveKey();
	}

	@Test(expected = AbortException.class)
	public void testReceiveKeyThrowsExceptionIfProtocolVersionIsUnrecognised()
			throws Exception {
		byte unknownVersion = (byte) (PROTOCOL_VERSION + 1);
		byte[] key = getRandomBytes(123);

		setup();
		context.checking(new Expectations() {{
			oneOf(recordReader).readRecord();
			will(returnValue(new Record(unknownVersion, KEY, key)));
		}});

		kat.receiveKey();
	}

	@Test(expected = AbortException.class)
	public void testReceiveKeyThrowsExceptionIfAbortIsReceived()
			throws Exception {
		setup();
		context.checking(new Expectations() {{
			oneOf(recordReader).readRecord();
			will(returnValue(new Record(PROTOCOL_VERSION, ABORT, new byte[0])));
		}});

		kat.receiveKey();
	}

	@Test(expected = AbortException.class)
	public void testReceiveKeyThrowsExceptionIfConfirmIsReceived()
			throws Exception {
		byte[] confirm = getRandomBytes(123);

		setup();
		context.checking(new Expectations() {{
			oneOf(recordReader).readRecord();
			will(returnValue(new Record(PROTOCOL_VERSION, CONFIRM, confirm)));
		}});

		kat.receiveKey();
	}

	@Test
	public void testReceiveKeySkipsUnrecognisedRecordTypes() throws Exception {
		byte type1 = (byte) (ABORT + 1);
		byte[] payload1 = getRandomBytes(123);
		Record unknownRecord1 = new Record(PROTOCOL_VERSION, type1, payload1);
		byte type2 = (byte) (ABORT + 2);
		byte[] payload2 = new byte[0];
		Record unknownRecord2 = new Record(PROTOCOL_VERSION, type2, payload2);
		byte[] key = getRandomBytes(123);
		Record keyRecord = new Record(PROTOCOL_VERSION, KEY, key);

		setup();
		context.checking(new Expectations() {{
			oneOf(recordReader).readRecord();
			will(returnValue(unknownRecord1));
			oneOf(recordReader).readRecord();
			will(returnValue(unknownRecord2));
			oneOf(recordReader).readRecord();
			will(returnValue(keyRecord));
		}});

		assertArrayEquals(key, kat.receiveKey());
	}

	@Test(expected = AbortException.class)
	public void testReceiveConfirmThrowsExceptionIfAtEndOfStream()
			throws Exception {
		setup();
		context.checking(new Expectations() {{
			oneOf(recordReader).readRecord();
			will(throwException(new EOFException()));
		}});

		kat.receiveConfirm();
	}

	@Test(expected = AbortException.class)
	public void testReceiveConfirmThrowsExceptionIfProtocolVersionIsUnrecognised()
			throws Exception {
		byte unknownVersion = (byte) (PROTOCOL_VERSION + 1);
		byte[] confirm = getRandomBytes(123);

		setup();
		context.checking(new Expectations() {{
			oneOf(recordReader).readRecord();
			will(returnValue(new Record(unknownVersion, CONFIRM, confirm)));
		}});

		kat.receiveConfirm();
	}

	@Test(expected = AbortException.class)
	public void testReceiveConfirmThrowsExceptionIfAbortIsReceived()
			throws Exception {
		setup();
		context.checking(new Expectations() {{
			oneOf(recordReader).readRecord();
			will(returnValue(new Record(PROTOCOL_VERSION, ABORT, new byte[0])));
		}});

		kat.receiveConfirm();
	}

	@Test(expected = AbortException.class)
	public void testReceiveConfirmThrowsExceptionIfKeyIsReceived()
			throws Exception {
		byte[] key = getRandomBytes(123);

		setup();
		context.checking(new Expectations() {{
			oneOf(recordReader).readRecord();
			will(returnValue(new Record(PROTOCOL_VERSION, KEY, key)));
		}});

		kat.receiveConfirm();
	}

	@Test
	public void testReceiveConfirmSkipsUnrecognisedRecordTypes()
			throws Exception {
		byte type1 = (byte) (ABORT + 1);
		byte[] payload1 = getRandomBytes(123);
		Record unknownRecord1 = new Record(PROTOCOL_VERSION, type1, payload1);
		byte type2 = (byte) (ABORT + 2);
		byte[] payload2 = new byte[0];
		Record unknownRecord2 = new Record(PROTOCOL_VERSION, type2, payload2);
		byte[] confirm = getRandomBytes(123);
		Record confirmRecord = new Record(PROTOCOL_VERSION, CONFIRM, confirm);

		setup();
		context.checking(new Expectations() {{
			oneOf(recordReader).readRecord();
			will(returnValue(unknownRecord1));
			oneOf(recordReader).readRecord();
			will(returnValue(unknownRecord2));
			oneOf(recordReader).readRecord();
			will(returnValue(confirmRecord));
		}});

		assertArrayEquals(confirm, kat.receiveConfirm());
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

	private void assertRecordEquals(byte expectedVersion, byte expectedType,
			byte[] expectedPayload, Record actual) {
		assertEquals(expectedVersion, actual.getProtocolVersion());
		assertEquals(expectedType, actual.getRecordType());
		assertArrayEquals(expectedPayload, actual.getPayload());
	}
}

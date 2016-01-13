package org.briarproject.sync;

import org.briarproject.BriarTestCase;
import org.briarproject.api.FormatException;
import org.briarproject.api.crypto.MessageDigest;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ConsumersTest extends BriarTestCase {

	@Test
	public void testDigestingConsumer() throws Exception {
		byte[] data = new byte[1234];
		// Generate some random data and digest it
		new Random().nextBytes(data);
		MessageDigest messageDigest = new TestMessageDigest();
		messageDigest.update(data);
		byte[] dig = messageDigest.digest();
		// Check that feeding a DigestingConsumer generates the same digest
		DigestingConsumer dc = new DigestingConsumer(messageDigest);
		dc.write(data[0]);
		dc.write(data, 1, data.length - 2);
		dc.write(data[data.length - 1]);
		byte[] dig1 = messageDigest.digest();
		assertArrayEquals(dig, dig1);
	}

	@Test(expected = FormatException.class)
	public void testCountingConsumer() throws Exception {
		byte[] data = new byte[1234];
		CountingConsumer cc = new CountingConsumer(data.length);
		cc.write(data[0]);
		cc.write(data, 1, data.length - 2);
		cc.write(data[data.length - 1]);
		assertEquals(data.length, cc.getCount());
		cc.write((byte) 0);
	}

	@Test
	public void testCopyingConsumer() throws Exception {
		byte[] data = new byte[1234];
		new Random().nextBytes(data);
		// Check that a CopyingConsumer creates a faithful copy
		CopyingConsumer cc = new CopyingConsumer();
		cc.write(data[0]);
		cc.write(data, 1, data.length - 2);
		cc.write(data[data.length - 1]);
		assertArrayEquals(data, cc.getCopy());
	}

	private static class TestMessageDigest implements MessageDigest {

		private final java.security.MessageDigest delegate;

		private TestMessageDigest() throws GeneralSecurityException {
			delegate = java.security.MessageDigest.getInstance("SHA-256");
		}

		public byte[] digest() {
			return delegate.digest();
		}

		public byte[] digest(byte[] input) {
			return delegate.digest(input);
		}

		public int digest(byte[] buf, int offset, int len) {
			byte[] digest = digest();
			len = Math.min(len, digest.length);
			System.arraycopy(digest, 0, buf, offset, len);
			return len;
		}

		public int getDigestLength() {
			return delegate.getDigestLength();
		}

		public void reset() {
			delegate.reset();
		}

		public void update(byte input) {
			delegate.update(input);
		}

		public void update(byte[] input) {
			delegate.update(input);
		}

		public void update(byte[] input, int offset, int len) {
			delegate.update(input, offset, len);
		}
	}
}

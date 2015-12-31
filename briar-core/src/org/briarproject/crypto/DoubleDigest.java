package org.briarproject.crypto;

import org.briarproject.api.crypto.MessageDigest;
import org.spongycastle.crypto.Digest;

class DoubleDigest implements MessageDigest {

	private final Digest delegate;

	DoubleDigest(Digest delegate) {
		this.delegate = delegate;
	}

	public byte[] digest() {
		byte[] digest = new byte[delegate.getDigestSize()];
		delegate.doFinal(digest, 0); // h(m)
		delegate.update(digest, 0, digest.length);
		delegate.doFinal(digest, 0); // h(h(m))
		return digest;
	}

	public byte[] digest(byte[] input) {
		delegate.update(input, 0, input.length);
		return digest();
	}

	public int digest(byte[] buf, int offset, int len) {
		byte[] digest = digest();
		len = Math.min(len, digest.length);
		System.arraycopy(digest, 0, buf, offset, len);
		return len;
	}

	public int getDigestLength() {
		return delegate.getDigestSize();
	}

	public void reset() {
		delegate.reset();
	}

	public void update(byte input) {
		delegate.update(input);
	}

	public void update(byte[] input) {
		delegate.update(input, 0, input.length);
	}

	public void update(byte[] input, int offset, int len) {
		delegate.update(input, offset, len);
	}
}

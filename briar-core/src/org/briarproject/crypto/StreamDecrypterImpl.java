package org.briarproject.crypto;

import org.briarproject.api.FormatException;
import org.briarproject.api.crypto.SecretKey;
import org.briarproject.api.crypto.StreamDecrypter;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import static org.briarproject.api.transport.TransportConstants.FRAME_HEADER_LENGTH;
import static org.briarproject.api.transport.TransportConstants.FRAME_HEADER_PLAINTEXT_LENGTH;
import static org.briarproject.api.transport.TransportConstants.FRAME_NONCE_LENGTH;
import static org.briarproject.api.transport.TransportConstants.MAC_LENGTH;
import static org.briarproject.api.transport.TransportConstants.MAX_FRAME_LENGTH;
import static org.briarproject.api.transport.TransportConstants.MAX_PAYLOAD_LENGTH;
import static org.briarproject.api.transport.TransportConstants.STREAM_HEADER_IV_LENGTH;
import static org.briarproject.api.transport.TransportConstants.STREAM_HEADER_LENGTH;

class StreamDecrypterImpl implements StreamDecrypter {

	private final InputStream in;
	private final AuthenticatedCipher cipher;
	private final SecretKey streamHeaderKey;
	private final byte[] frameNonce, frameHeader, frameCiphertext;

	private SecretKey frameKey;
	private long frameNumber;
	private boolean finalFrame;

	StreamDecrypterImpl(InputStream in, AuthenticatedCipher cipher,
			SecretKey streamHeaderKey) {
		this.in = in;
		this.cipher = cipher;
		this.streamHeaderKey = streamHeaderKey;
		frameNonce = new byte[FRAME_NONCE_LENGTH];
		frameHeader = new byte[FRAME_HEADER_PLAINTEXT_LENGTH];
		frameCiphertext = new byte[MAX_FRAME_LENGTH];
		frameKey = null;
		frameNumber = 0;
		finalFrame = false;
	}

	public int readFrame(byte[] payload) throws IOException {
		// The buffer must be big enough for a full-size frame
		if (payload.length < MAX_PAYLOAD_LENGTH)
			throw new IllegalArgumentException();
		if (finalFrame) return -1;
		// Don't allow the frame counter to wrap
		if (frameNumber < 0) throw new IOException();
		// Read the stream header if required
		if (frameKey == null) readStreamHeader();
		// Read the frame header
		int offset = 0;
		while (offset < FRAME_HEADER_LENGTH) {
			int read = in.read(frameCiphertext, offset,
					FRAME_HEADER_LENGTH - offset);
			if (read == -1) throw new EOFException();
			offset += read;
		}
		// Decrypt and authenticate the frame header
		FrameEncoder.encodeNonce(frameNonce, frameNumber, true);
		try {
			cipher.init(false, frameKey, frameNonce);
			int decrypted = cipher.process(frameCiphertext, 0,
					FRAME_HEADER_LENGTH, frameHeader, 0);
			if (decrypted != FRAME_HEADER_PLAINTEXT_LENGTH)
				throw new RuntimeException();
		} catch (GeneralSecurityException e) {
			throw new FormatException();
		}
		// Decode and validate the frame header
		finalFrame = FrameEncoder.isFinalFrame(frameHeader);
		int payloadLength = FrameEncoder.getPayloadLength(frameHeader);
		int paddingLength = FrameEncoder.getPaddingLength(frameHeader);
		if (payloadLength + paddingLength > MAX_PAYLOAD_LENGTH)
			throw new FormatException();
		// Read the payload and padding
		int frameLength = FRAME_HEADER_LENGTH + payloadLength + paddingLength
				+ MAC_LENGTH;
		while (offset < frameLength) {
			int read = in.read(frameCiphertext, offset, frameLength - offset);
			if (read == -1) throw new EOFException();
			offset += read;
		}
		// Decrypt and authenticate the payload and padding
		FrameEncoder.encodeNonce(frameNonce, frameNumber, false);
		try {
			cipher.init(false, frameKey, frameNonce);
			int decrypted = cipher.process(frameCiphertext, FRAME_HEADER_LENGTH,
					payloadLength + paddingLength + MAC_LENGTH, payload, 0);
			if (decrypted != payloadLength + paddingLength)
				throw new RuntimeException();
		} catch (GeneralSecurityException e) {
			throw new FormatException();
		}
		// If there's any padding it must be all zeroes
		for (int i = 0; i < paddingLength; i++)
			if (payload[payloadLength + i] != 0) throw new FormatException();
		frameNumber++;
		return payloadLength;
	}

	private void readStreamHeader() throws IOException {
		byte[] streamHeaderIv = new byte[STREAM_HEADER_IV_LENGTH];
		byte[] streamHeaderCiphertext = new byte[STREAM_HEADER_LENGTH];
		byte[] streamHeaderPlaintext = new byte[SecretKey.LENGTH];
		// Read the stream header
		int offset = 0;
		while (offset < STREAM_HEADER_LENGTH) {
			int read = in.read(streamHeaderCiphertext, offset,
					STREAM_HEADER_LENGTH - offset);
			if (read == -1) throw new EOFException();
			offset += read;
		}
		// Decrypt and authenticate the stream header
		System.arraycopy(streamHeaderCiphertext, 0, streamHeaderIv, 0,
				STREAM_HEADER_IV_LENGTH);
		try {
			cipher.init(false, streamHeaderKey, streamHeaderIv);
			int decrypted = cipher.process(streamHeaderCiphertext,
					STREAM_HEADER_IV_LENGTH, SecretKey.LENGTH + MAC_LENGTH,
					streamHeaderPlaintext, 0);
			if (decrypted != SecretKey.LENGTH) throw new RuntimeException();
		} catch (GeneralSecurityException e) {
			throw new FormatException();
		}
		frameKey = new SecretKey(streamHeaderPlaintext);
	}
}
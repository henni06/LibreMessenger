package org.briarproject.api.crypto;

import org.briarproject.api.TransportId;
import org.briarproject.api.transport.TransportKeys;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public interface CryptoComponent {

	SecretKey generateSecretKey();

	MessageDigest getMessageDigest();

	PseudoRandom getPseudoRandom(int seed1, int seed2);

	SecureRandom getSecureRandom();

	Signature getSignature();

	KeyPair generateAgreementKeyPair();

	KeyParser getAgreementKeyParser();

	KeyPair generateSignatureKeyPair();

	KeyParser getSignatureKeyParser();

	/** Generates a random invitation code. */
	int generateBTInvitationCode();

	/**
	 * Derives a confirmation code from the given master secret.
	 * @param alice whether the code is for use by Alice or Bob.
	 */
	int deriveBTConfirmationCode(SecretKey master, boolean alice);

	/**
	 * Derives a stream header key from the given master secret.
	 * @param alice whether the key is for use by Alice or Bob.
	 */
	SecretKey deriveHeaderKey(SecretKey master, boolean alice);

	/**
	 * Derives a nonce from the given master secret for one of the parties to
	 * sign.
	 * @param alice whether the nonce is for use by Alice or Bob.
	 */
	byte[] deriveSignatureNonce(SecretKey master, boolean alice);

	/**
	 * Derives a commitment to the provided public key.
	 * <p/>
	 * Part of BQP.
	 *
	 * @param publicKey the public key
	 * @return the commitment to the provided public key.
	 */
	byte[] deriveKeyCommitment(byte[] publicKey);

	/**
	 * Derives a common shared secret from two public keys and one of the
	 * corresponding private keys.
	 * <p/>
	 * Part of BQP.
	 *
	 * @param theirPublicKey the ephemeral public key of the remote party
	 * @param ourKeyPair our ephemeral keypair
	 * @param alice true if ourKeyPair belongs to Alice
	 * @return the shared secret
	 * @throws GeneralSecurityException
	 */
	SecretKey deriveSharedSecret(byte[] theirPublicKey, KeyPair ourKeyPair,
			boolean alice) throws GeneralSecurityException;

	/**
	 * Derives the content of a confirmation record.
	 * <p/>
	 * Part of BQP.
	 *
	 * @param sharedSecret the common shared secret
	 * @param theirPayload the commit payload from the remote party
	 * @param ourPayload the commit payload we sent
	 * @param theirPublicKey the ephemeral public key of the remote party
	 * @param ourKeyPair our ephemeral keypair
	 * @param alice true if ourKeyPair belongs to Alice
	 * @param aliceRecord true if the confirmation record is for use by Alice
	 * @return the confirmation record
	 */
	byte[] deriveConfirmationRecord(SecretKey sharedSecret,
			byte[] theirPayload, byte[] ourPayload,
			byte[] theirPublicKey, KeyPair ourKeyPair,
			boolean alice, boolean aliceRecord);

	/**
	 * Derives a master secret from the given shared secret.
	 * <p/>
	 * Part of BQP.
	 *
	 * @param sharedSecret the common shared secret
	 * @return the master secret
	 */
	SecretKey deriveMasterSecret(SecretKey sharedSecret);

	/**
	 * Derives a master secret from two public keys and one of the corresponding
	 * private keys.
	 * <p/>
	 * This is a helper method that calls
	 * deriveMasterSecret(deriveSharedSecret(theirPublicKey, ourKeyPair, alice))
	 *
	 * @param theirPublicKey the ephemeral public key of the remote party
	 * @param ourKeyPair our ephemeral keypair
	 * @param alice true if ourKeyPair belongs to Alice
	 * @return the shared secret
	 * @throws GeneralSecurityException
	 */
	SecretKey deriveMasterSecret(byte[] theirPublicKey, KeyPair ourKeyPair,
			boolean alice) throws GeneralSecurityException;

	/**
	 * Derives initial transport keys for the given transport in the given
	 * rotation period from the given master secret.
	 * @param alice whether the keys are for use by Alice or Bob.
	 */
	TransportKeys deriveTransportKeys(TransportId t, SecretKey master,
			long rotationPeriod, boolean alice);

	/**
	 * Rotates the given transport keys to the given rotation period. If the
	 * keys are for a future rotation period they are not rotated.
	 */
	TransportKeys rotateTransportKeys(TransportKeys k, long rotationPeriod);

	/** Encodes the pseudo-random tag that is used to recognise a stream. */
	void encodeTag(byte[] tag, SecretKey tagKey, long streamNumber);

	/**
	 * Returns the hash of the given inputs. The inputs are unambiguously
	 * combined by prefixing each input with its length.
	 */
	byte[] hash(byte[]... inputs);

	/**
	 * Encrypts and authenticates the given plaintext so it can be written to
	 * storage. The encryption and authentication keys are derived from the
	 * given password. The ciphertext will be decryptable using the same
	 * password after the app restarts.
	 */
	byte[] encryptWithPassword(byte[] plaintext, String password);

	/**
	 * Decrypts and authenticates the given ciphertext that has been read from
	 * storage. The encryption and authentication keys are derived from the
	 * given password. Returns null if the ciphertext cannot be decrypted and
	 * authenticated (for example, if the password is wrong).
	 */
	byte[] decryptWithPassword(byte[] ciphertext, String password);
}

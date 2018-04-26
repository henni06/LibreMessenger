package org.briarproject.briar.introduction;

import org.briarproject.bramble.api.Bytes;
import org.briarproject.bramble.api.FormatException;
import org.briarproject.bramble.api.client.ClientHelper;
import org.briarproject.bramble.api.crypto.CryptoComponent;
import org.briarproject.bramble.api.crypto.KeyPair;
import org.briarproject.bramble.api.crypto.KeyParser;
import org.briarproject.bramble.api.crypto.PrivateKey;
import org.briarproject.bramble.api.crypto.PublicKey;
import org.briarproject.bramble.api.crypto.SecretKey;
import org.briarproject.bramble.api.data.BdfList;
import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.identity.AuthorId;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.properties.TransportProperties;
import org.briarproject.briar.api.client.SessionId;

import java.security.GeneralSecurityException;
import java.util.Map;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.briarproject.briar.api.introduction.IntroductionConstants.LABEL_ACTIVATE_MAC;
import static org.briarproject.briar.api.introduction.IntroductionConstants.LABEL_ALICE_MAC_KEY;
import static org.briarproject.briar.api.introduction.IntroductionConstants.LABEL_AUTH_MAC;
import static org.briarproject.briar.api.introduction.IntroductionConstants.LABEL_AUTH_NONCE;
import static org.briarproject.briar.api.introduction.IntroductionConstants.LABEL_AUTH_SIGN;
import static org.briarproject.briar.api.introduction.IntroductionConstants.LABEL_BOB_MAC_KEY;
import static org.briarproject.briar.api.introduction.IntroductionConstants.LABEL_MASTER_KEY;
import static org.briarproject.briar.api.introduction.IntroductionConstants.LABEL_SESSION_ID;
import static org.briarproject.briar.api.introduction.IntroductionManager.CLIENT_VERSION;

@Immutable
@NotNullByDefault
class IntroductionCryptoImpl implements IntroductionCrypto {

	private final CryptoComponent crypto;
	private final ClientHelper clientHelper;

	@Inject
	IntroductionCryptoImpl(
			CryptoComponent crypto,
			ClientHelper clientHelper) {
		this.crypto = crypto;
		this.clientHelper = clientHelper;
	}

	@Override
	public SessionId getSessionId(Author introducer, Author local,
			Author remote) {
		boolean isAlice = isAlice(local.getId(), remote.getId());
		byte[] hash = crypto.hash(
				LABEL_SESSION_ID,
				introducer.getId().getBytes(),
				isAlice ? local.getId().getBytes() : remote.getId().getBytes(),
				isAlice ? remote.getId().getBytes() : local.getId().getBytes()
		);
		return new SessionId(hash);
	}

	@Override
	public KeyPair generateKeyPair() {
		return crypto.generateAgreementKeyPair();
	}

	@Override
	public boolean isAlice(AuthorId local, AuthorId remote) {
		byte[] a = local.getBytes();
		byte[] b = remote.getBytes();
		return Bytes.COMPARATOR.compare(new Bytes(a), new Bytes(b)) < 0;
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public SecretKey deriveMasterKey(IntroduceeSession s)
			throws GeneralSecurityException {
		return deriveMasterKey(
				s.getLocal().ephemeralPublicKey,
				s.getLocal().ephemeralPrivateKey,
				s.getRemote().ephemeralPublicKey,
				s.getLocal().alice
		);
	}

	SecretKey deriveMasterKey(byte[] publicKey, byte[] privateKey,
			byte[] remotePublicKey, boolean alice)
			throws GeneralSecurityException {
		KeyParser kp = crypto.getAgreementKeyParser();
		PublicKey remoteEphemeralPublicKey = kp.parsePublicKey(remotePublicKey);
		PublicKey ephemeralPublicKey = kp.parsePublicKey(publicKey);
		PrivateKey ephemeralPrivateKey = kp.parsePrivateKey(privateKey);
		KeyPair keyPair = new KeyPair(ephemeralPublicKey, ephemeralPrivateKey);
		return crypto.deriveSharedSecret(
				LABEL_MASTER_KEY,
				remoteEphemeralPublicKey,
				keyPair,
				new byte[] {CLIENT_VERSION},
				alice ? publicKey : remotePublicKey,
				alice ? remotePublicKey : publicKey
		);
	}

	@Override
	public SecretKey deriveMacKey(SecretKey masterKey, boolean alice) {
		return crypto.deriveKey(
				alice ? LABEL_ALICE_MAC_KEY : LABEL_BOB_MAC_KEY,
				masterKey
		);
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public byte[] authMac(SecretKey macKey, IntroduceeSession s,
			AuthorId localAuthorId, boolean alice) {
		return authMac(macKey, s.getIntroducer().getId(), localAuthorId,
				s.getRemote().author.getId(), s.getLocal().acceptTimestamp,
				s.getRemote().acceptTimestamp, s.getLocal().ephemeralPublicKey,
				s.getRemote().ephemeralPublicKey,
				s.getLocal().transportProperties,
				s.getRemote().transportProperties, alice);
	}

	byte[] authMac(SecretKey macKey, AuthorId introducerId,
			AuthorId localAuthorId, AuthorId remoteAuthorId,
			long acceptTimestamp, long remoteAcceptTimestamp,
			byte[] ephemeralPublicKey, byte[] remoteEphemeralPublicKey,
			Map<TransportId, TransportProperties> transportProperties,
			Map<TransportId, TransportProperties> remoteTransportProperties,
			boolean alice) {
		byte[] inputs =
				getAuthMacInputs(introducerId, localAuthorId, remoteAuthorId,
						acceptTimestamp, remoteAcceptTimestamp,
						ephemeralPublicKey, remoteEphemeralPublicKey,
						transportProperties, remoteTransportProperties, alice);
		return crypto.mac(
				LABEL_AUTH_MAC,
				macKey,
				inputs
		);
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public void verifyAuthMac(byte[] mac, IntroduceeSession s,
			AuthorId localAuthorId)
			throws GeneralSecurityException {
		boolean alice = isAlice(localAuthorId, s.getRemote().author.getId());
		verifyAuthMac(mac, new SecretKey(s.getRemote().macKey),
				s.getIntroducer().getId(), localAuthorId,
				s.getRemote().author.getId(), s.getLocal().acceptTimestamp,
				s.getRemote().acceptTimestamp, s.getLocal().ephemeralPublicKey,
				s.getRemote().ephemeralPublicKey,
				s.getLocal().transportProperties,
				s.getRemote().transportProperties, !alice);
	}

	void verifyAuthMac(byte[] mac, SecretKey macKey,
			AuthorId introducerId, AuthorId localAuthorId,
			AuthorId remoteAuthorId, long acceptTimestamp,
			long remoteAcceptTimestamp, byte[] ephemeralPublicKey,
			byte[] remoteEphemeralPublicKey,
			Map<TransportId, TransportProperties> transportProperties,
			Map<TransportId, TransportProperties> remoteTransportProperties,
			boolean alice) throws GeneralSecurityException {
		byte[] inputs =
				getAuthMacInputs(introducerId, localAuthorId, remoteAuthorId,
						acceptTimestamp, remoteAcceptTimestamp,
						ephemeralPublicKey, remoteEphemeralPublicKey,
						transportProperties, remoteTransportProperties, !alice);
		if (!crypto.verifyMac(mac, LABEL_AUTH_MAC, macKey, inputs)) {
			throw new GeneralSecurityException();
		}
	}

	private byte[] getAuthMacInputs(AuthorId introducerId,
			AuthorId localAuthorId, AuthorId remoteAuthorId,
			long acceptTimestamp, long remoteAcceptTimestamp,
			byte[] ephemeralPublicKey, byte[] remoteEphemeralPublicKey,
			Map<TransportId, TransportProperties> transportProperties,
			Map<TransportId, TransportProperties> remoteTransportProperties,
			boolean alice) {
		BdfList localInfo = BdfList.of(
				localAuthorId,
				acceptTimestamp,
				ephemeralPublicKey,
				clientHelper.toDictionary(transportProperties)
		);
		BdfList remoteInfo = BdfList.of(
				remoteAuthorId,
				remoteAcceptTimestamp,
				remoteEphemeralPublicKey,
				clientHelper.toDictionary(remoteTransportProperties)
		);
		BdfList macList = BdfList.of(
				introducerId,
				alice ? localInfo : remoteInfo,
				alice ? remoteInfo : localInfo
		);
		try {
			return clientHelper.toByteArray(macList);
		} catch (FormatException e) {
			throw new AssertionError();
		}
	}

	@Override
	public byte[] sign(SecretKey macKey, byte[] privateKey)
			throws GeneralSecurityException {
		return crypto.sign(
				LABEL_AUTH_SIGN,
				getNonce(macKey),
				privateKey
		);
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public void verifySignature(byte[] signature, IntroduceeSession s,
			AuthorId localAuthorId) throws GeneralSecurityException {
		SecretKey macKey = new SecretKey(s.getRemote().macKey);
		verifySignature(macKey, s.getRemote().author.getPublicKey(), signature);
	}

	void verifySignature(SecretKey macKey, byte[] publicKey,
			byte[] signature) throws GeneralSecurityException {
		byte[] nonce = getNonce(macKey);
		if (!crypto.verifySignature(signature, LABEL_AUTH_SIGN, nonce,
				publicKey)) {
			throw new GeneralSecurityException();
		}
	}

	private byte[] getNonce(SecretKey macKey) {
		return crypto.mac(LABEL_AUTH_NONCE, macKey);
	}

	@Override
	public byte[] activateMac(IntroduceeSession s) {
		if (s.getLocal().macKey == null)
			throw new AssertionError("Local MAC key is null");
		return activateMac(new SecretKey(s.getLocal().macKey));
	}

	byte[] activateMac(SecretKey macKey) {
		return crypto.mac(
				LABEL_ACTIVATE_MAC,
				macKey
		);
	}

	@Override
	public void verifyActivateMac(byte[] mac, IntroduceeSession s)
			throws GeneralSecurityException {
		if (s.getRemote().macKey == null)
			throw new AssertionError("Remote MAC key is null");
		verifyActivateMac(mac, new SecretKey(s.getRemote().macKey));
	}

	void verifyActivateMac(byte[] mac, SecretKey macKey)
			throws GeneralSecurityException {
		if (!crypto.verifyMac(mac, LABEL_ACTIVATE_MAC, macKey)) {
			throw new GeneralSecurityException();
		}
	}

}

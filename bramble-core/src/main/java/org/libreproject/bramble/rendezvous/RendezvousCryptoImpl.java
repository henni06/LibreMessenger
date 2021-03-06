package org.libreproject.bramble.rendezvous;

import org.libreproject.bramble.api.crypto.CryptoComponent;
import org.libreproject.bramble.api.crypto.SecretKey;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.rendezvous.KeyMaterialSource;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.libreproject.bramble.rendezvous.RendezvousConstants.KEY_MATERIAL_LABEL;
import static org.libreproject.bramble.rendezvous.RendezvousConstants.PROTOCOL_VERSION;
import static org.libreproject.bramble.rendezvous.RendezvousConstants.RENDEZVOUS_KEY_LABEL;
import static org.libreproject.bramble.util.StringUtils.toUtf8;

@Immutable
@NotNullByDefault
class RendezvousCryptoImpl implements RendezvousCrypto {

	private final CryptoComponent crypto;

	@Inject
	RendezvousCryptoImpl(CryptoComponent crypto) {
		this.crypto = crypto;
	}

	@Override
	public SecretKey deriveRendezvousKey(SecretKey staticMasterKey) {
		return crypto.deriveKey(RENDEZVOUS_KEY_LABEL, staticMasterKey,
				new byte[] {PROTOCOL_VERSION});
	}

	@Override
	public KeyMaterialSource createKeyMaterialSource(SecretKey rendezvousKey,
			TransportId t) {
		SecretKey sourceKey = crypto.deriveKey(KEY_MATERIAL_LABEL,
				rendezvousKey, toUtf8(t.getString()));
		return new KeyMaterialSourceImpl(sourceKey);
	}
}

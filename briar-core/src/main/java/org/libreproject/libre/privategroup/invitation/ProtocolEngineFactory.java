package org.libreproject.libre.privategroup.invitation;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
interface ProtocolEngineFactory {

	ProtocolEngine<CreatorSession> createCreatorEngine();

	ProtocolEngine<InviteeSession> createInviteeEngine();

	ProtocolEngine<PeerSession> createPeerEngine();
}

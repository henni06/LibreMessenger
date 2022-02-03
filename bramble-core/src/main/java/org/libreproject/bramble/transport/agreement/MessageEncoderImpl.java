package org.libreproject.bramble.transport.agreement;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.crypto.PublicKey;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.data.BdfEntry;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.api.system.Clock;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.libreproject.bramble.transport.agreement.MessageType.ACTIVATE;
import static org.libreproject.bramble.transport.agreement.MessageType.KEY;
import static org.libreproject.bramble.transport.agreement.TransportKeyAgreementConstants.MSG_KEY_IS_SESSION;
import static org.libreproject.bramble.transport.agreement.TransportKeyAgreementConstants.MSG_KEY_LOCAL;
import static org.libreproject.bramble.transport.agreement.TransportKeyAgreementConstants.MSG_KEY_MESSAGE_TYPE;
import static org.libreproject.bramble.transport.agreement.TransportKeyAgreementConstants.MSG_KEY_TRANSPORT_ID;

@Immutable
@NotNullByDefault
class MessageEncoderImpl implements MessageEncoder {

	private final ClientHelper clientHelper;
	private final Clock clock;

	@Inject
	MessageEncoderImpl(ClientHelper clientHelper, Clock clock) {
		this.clientHelper = clientHelper;
		this.clock = clock;
	}

	@Override
	public Message encodeKeyMessage(GroupId contactGroupId,
			TransportId transportId, PublicKey publicKey) {
		BdfList body = BdfList.of(
				KEY.getValue(),
				transportId.getString(),
				publicKey.getEncoded());
		return encodeMessage(contactGroupId, body);
	}

	@Override
	public Message encodeActivateMessage(GroupId contactGroupId,
			TransportId transportId, MessageId previousMessageId) {
		BdfList body = BdfList.of(
				ACTIVATE.getValue(),
				transportId.getString(),
				previousMessageId);
		return encodeMessage(contactGroupId, body);
	}

	@Override
	public BdfDictionary encodeMessageMetadata(TransportId transportId,
			MessageType type, boolean local) {
		return BdfDictionary.of(
				new BdfEntry(MSG_KEY_IS_SESSION, false),
				new BdfEntry(MSG_KEY_TRANSPORT_ID, transportId.getString()),
				new BdfEntry(MSG_KEY_MESSAGE_TYPE, type.getValue()),
				new BdfEntry(MSG_KEY_LOCAL, local));
	}

	private Message encodeMessage(GroupId contactGroupId, BdfList body) {
		try {
			return clientHelper.createMessage(contactGroupId,
					clock.currentTimeMillis(), clientHelper.toByteArray(body));
		} catch (FormatException e) {
			throw new AssertionError();
		}
	}
}

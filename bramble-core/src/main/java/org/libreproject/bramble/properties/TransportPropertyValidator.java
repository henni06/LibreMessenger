package org.libreproject.bramble.properties;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.client.BdfMessageContext;
import org.libreproject.bramble.api.client.BdfMessageValidator;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.Group;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.system.Clock;

import javax.annotation.concurrent.Immutable;

import static org.libreproject.bramble.api.plugin.TransportId.MAX_TRANSPORT_ID_LENGTH;
import static org.libreproject.bramble.api.properties.TransportPropertyConstants.MSG_KEY_LOCAL;
import static org.libreproject.bramble.api.properties.TransportPropertyConstants.MSG_KEY_TRANSPORT_ID;
import static org.libreproject.bramble.api.properties.TransportPropertyConstants.MSG_KEY_VERSION;
import static org.libreproject.bramble.util.ValidationUtils.checkLength;
import static org.libreproject.bramble.util.ValidationUtils.checkSize;

@Immutable
@NotNullByDefault
class TransportPropertyValidator extends BdfMessageValidator {

	TransportPropertyValidator(ClientHelper clientHelper,
			MetadataEncoder metadataEncoder, Clock clock) {
		super(clientHelper, metadataEncoder, clock);
	}

	@Override
	protected BdfMessageContext validateMessage(Message m, Group g,
			BdfList body) throws FormatException {
		// Transport ID, version, properties
		checkSize(body, 3);
		// Transport ID
		String transportId = body.getString(0);
		checkLength(transportId, 1, MAX_TRANSPORT_ID_LENGTH);
		// Version
		long version = body.getLong(1);
		if (version < 0) throw new FormatException();
		// Properties
		BdfDictionary dictionary = body.getDictionary(2);
		clientHelper.parseAndValidateTransportProperties(dictionary);
		// Return the metadata
		BdfDictionary meta = new BdfDictionary();
		meta.put(MSG_KEY_TRANSPORT_ID, transportId);
		meta.put(MSG_KEY_VERSION, version);
		meta.put(MSG_KEY_LOCAL, false);
		return new BdfMessageContext(meta);
	}
}

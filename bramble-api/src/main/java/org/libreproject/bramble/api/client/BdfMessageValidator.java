package org.libreproject.bramble.api.client;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.db.Metadata;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.Group;
import org.libreproject.bramble.api.sync.InvalidMessageException;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageContext;
import org.libreproject.bramble.api.sync.validation.MessageValidator;
import org.libreproject.bramble.api.system.Clock;

import java.util.logging.Logger;

import javax.annotation.concurrent.Immutable;

import static org.libreproject.bramble.api.transport.TransportConstants.MAX_CLOCK_DIFFERENCE;

@Immutable
@NotNullByDefault
public abstract class BdfMessageValidator implements MessageValidator {

	protected static final Logger LOG =
			Logger.getLogger(BdfMessageValidator.class.getName());

	protected final ClientHelper clientHelper;
	protected final MetadataEncoder metadataEncoder;
	protected final Clock clock;

	protected BdfMessageValidator(ClientHelper clientHelper,
			MetadataEncoder metadataEncoder, Clock clock) {
		this.clientHelper = clientHelper;
		this.metadataEncoder = metadataEncoder;
		this.clock = clock;
	}

	protected abstract BdfMessageContext validateMessage(Message m, Group g,
			BdfList body) throws InvalidMessageException, FormatException;

	@Override
	public MessageContext validateMessage(Message m, Group g)
			throws InvalidMessageException {
		// Reject the message if it's too far in the future
		long now = clock.currentTimeMillis();
		if (m.getTimestamp() - now > MAX_CLOCK_DIFFERENCE) {
			throw new InvalidMessageException(
					"Timestamp is too far in the future");
		}
		try {
			BdfList bodyList = clientHelper.toList(m.getBody());
			BdfMessageContext result = validateMessage(m, g, bodyList);
			Metadata meta = metadataEncoder.encode(result.getDictionary());
			return new MessageContext(meta, result.getDependencies());
		} catch (FormatException e) {
			throw new InvalidMessageException(e);
		}
	}
}

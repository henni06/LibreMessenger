package org.libreproject.bramble.api.sync.validation;

import org.libreproject.bramble.api.sync.Group;
import org.libreproject.bramble.api.sync.InvalidMessageException;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageContext;

public interface MessageValidator {

	/**
	 * Validates the given message and returns its metadata and
	 * dependencies.
	 */
	MessageContext validateMessage(Message m, Group g)
			throws InvalidMessageException;
}

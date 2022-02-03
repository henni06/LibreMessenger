package org.libreproject.libre.introduction;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.libre.api.client.SessionId;

@NotNullByDefault
interface MessageParser {

	BdfDictionary getMessagesVisibleInUiQuery();

	BdfDictionary getRequestsAvailableToAnswerQuery(SessionId sessionId);

	MessageMetadata parseMetadata(BdfDictionary meta) throws FormatException;

	RequestMessage parseRequestMessage(Message m, BdfList body)
			throws FormatException;

	AcceptMessage parseAcceptMessage(Message m, BdfList body)
			throws FormatException;

	DeclineMessage parseDeclineMessage(Message m, BdfList body)
			throws FormatException;

	AuthMessage parseAuthMessage(Message m, BdfList body)
			throws FormatException;

	ActivateMessage parseActivateMessage(Message m, BdfList body)
			throws FormatException;

	AbortMessage parseAbortMessage(Message m, BdfList body)
			throws FormatException;

}

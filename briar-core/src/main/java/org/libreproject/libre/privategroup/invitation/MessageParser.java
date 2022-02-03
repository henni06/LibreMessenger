package org.libreproject.libre.privategroup.invitation;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;

@NotNullByDefault
interface MessageParser {

	BdfDictionary getMessagesVisibleInUiQuery();

	BdfDictionary getInvitesAvailableToAnswerQuery();

	BdfDictionary getInvitesAvailableToAnswerQuery(GroupId privateGroupId);

	MessageMetadata parseMetadata(BdfDictionary meta) throws FormatException;

	InviteMessage getInviteMessage(Transaction txn, MessageId m)
			throws DbException, FormatException;

	InviteMessage parseInviteMessage(Message m, BdfList body)
			throws FormatException;

	JoinMessage parseJoinMessage(Message m, BdfList body)
			throws FormatException;

	LeaveMessage parseLeaveMessage(Message m, BdfList body)
			throws FormatException;

	AbortMessage parseAbortMessage(Message m, BdfList body)
			throws FormatException;

}

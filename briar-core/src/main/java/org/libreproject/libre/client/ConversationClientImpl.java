package org.libreproject.libre.client;

import org.libreproject.bramble.api.client.BdfIncomingMessageHook;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.data.MetadataParser;
import org.libreproject.bramble.api.db.DatabaseComponent;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.api.client.MessageTracker;
import org.libreproject.libre.api.client.MessageTracker.GroupCount;
import org.libreproject.libre.api.conversation.ConversationManager.ConversationClient;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public abstract class ConversationClientImpl extends BdfIncomingMessageHook
		implements ConversationClient {

	protected final MessageTracker messageTracker;

	protected ConversationClientImpl(DatabaseComponent db,
			ClientHelper clientHelper, MetadataParser metadataParser,
			MessageTracker messageTracker) {
		super(db, clientHelper, metadataParser);
		this.messageTracker = messageTracker;
	}

	@Override
	public GroupCount getGroupCount(Transaction txn, ContactId contactId)
			throws DbException {
		Contact contact = db.getContact(txn, contactId);
		GroupId groupId = getContactGroup(contact).getId();
		return messageTracker.getGroupCount(txn, groupId);
	}

}

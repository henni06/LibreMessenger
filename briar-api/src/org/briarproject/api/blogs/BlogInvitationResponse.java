package org.briarproject.api.blogs;

import org.briarproject.api.clients.SessionId;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.sharing.InvitationResponse;
import org.briarproject.api.sync.GroupId;
import org.briarproject.api.sync.MessageId;

public class BlogInvitationResponse extends InvitationResponse {

	public BlogInvitationResponse(MessageId id, SessionId sessionId,
			GroupId groupId, ContactId contactId, boolean accept, long time,
			boolean local, boolean sent, boolean seen, boolean read) {

		super(id, sessionId, groupId, contactId, accept, time, local, sent,
				seen, read);
	}

}

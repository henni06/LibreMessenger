package org.libreproject.libre.sharing;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.db.DatabaseComponent;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.bramble.api.versioning.ClientVersioningManager;
import org.libreproject.libre.api.autodelete.AutoDeleteManager;
import org.libreproject.libre.api.blog.Blog;
import org.libreproject.libre.api.blog.BlogInvitationResponse;
import org.libreproject.libre.api.blog.BlogManager;
import org.libreproject.libre.api.blog.BlogSharingManager;
import org.libreproject.libre.api.blog.event.BlogInvitationRequestReceivedEvent;
import org.libreproject.libre.api.blog.event.BlogInvitationResponseReceivedEvent;
import org.libreproject.libre.api.conversation.ConversationManager;
import org.libreproject.libre.api.conversation.ConversationRequest;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
@NotNullByDefault
class BlogProtocolEngineImpl extends ProtocolEngineImpl<Blog> {

	private final BlogManager blogManager;
	private final InvitationFactory<Blog, BlogInvitationResponse>
			invitationFactory;

	@Inject
	BlogProtocolEngineImpl(
			DatabaseComponent db,
			ClientHelper clientHelper,
			ClientVersioningManager clientVersioningManager,
			MessageEncoder messageEncoder,
			MessageParser<Blog> messageParser,
			AutoDeleteManager autoDeleteManager,
			ConversationManager conversationManager,
			Clock clock,
			BlogManager blogManager,
			InvitationFactory<Blog, BlogInvitationResponse> invitationFactory) {
		super(db, clientHelper, clientVersioningManager, messageEncoder,
				messageParser, autoDeleteManager,
				conversationManager, clock, BlogSharingManager.CLIENT_ID,
				BlogSharingManager.MAJOR_VERSION, BlogManager.CLIENT_ID,
				BlogManager.MAJOR_VERSION);
		this.blogManager = blogManager;
		this.invitationFactory = invitationFactory;
	}

	@Override
	Event getInvitationRequestReceivedEvent(InviteMessage<Blog> m,
			ContactId contactId, boolean available, boolean canBeOpened) {
		ConversationRequest<Blog> request = invitationFactory
				.createInvitationRequest(false, false, true, false, m,
						contactId, available, canBeOpened,
						m.getAutoDeleteTimer());
		return new BlogInvitationRequestReceivedEvent(request, contactId);
	}

	@Override
	Event getInvitationResponseReceivedEvent(AcceptMessage m,
			ContactId contactId) {
		BlogInvitationResponse response = invitationFactory
				.createInvitationResponse(m.getId(), m.getContactGroupId(),
						m.getTimestamp(), false, false, false, false,
						true, m.getShareableId(), m.getAutoDeleteTimer(),
						false);
		return new BlogInvitationResponseReceivedEvent(response, contactId);
	}

	@Override
	Event getInvitationResponseReceivedEvent(DeclineMessage m,
			ContactId contactId) {
		BlogInvitationResponse response = invitationFactory
				.createInvitationResponse(m.getId(), m.getContactGroupId(),
						m.getTimestamp(), false, false, false, false,
						false, m.getShareableId(), m.getAutoDeleteTimer(),
						false);
		return new BlogInvitationResponseReceivedEvent(response, contactId);
	}

	@Override
	Event getAutoDeclineInvitationResponseReceivedEvent(Session s, Message m,
			ContactId contactId, long timer) {
		BlogInvitationResponse response = invitationFactory
				.createInvitationResponse(m.getId(), s.getContactGroupId(),
						m.getTimestamp(), true, false, false, true,
						false, s.getShareableId(), timer, true);
		return new BlogInvitationResponseReceivedEvent(response, contactId);
	}

	@Override
	protected void addShareable(Transaction txn, MessageId inviteId)
			throws DbException, FormatException {
		InviteMessage<Blog> invite =
				messageParser.getInviteMessage(txn, inviteId);
		blogManager.addBlog(txn, invite.getShareable());
	}

}

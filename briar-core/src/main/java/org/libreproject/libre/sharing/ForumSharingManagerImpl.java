package org.libreproject.libre.sharing;

import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.client.ContactGroupFactory;
import org.libreproject.bramble.api.data.MetadataParser;
import org.libreproject.bramble.api.db.DatabaseComponent;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.ClientId;
import org.libreproject.bramble.api.versioning.ClientVersioningManager;
import org.libreproject.libre.api.client.MessageTracker;
import org.libreproject.libre.api.forum.Forum;
import org.libreproject.libre.api.forum.ForumInvitationResponse;
import org.libreproject.libre.api.forum.ForumManager;
import org.libreproject.libre.api.forum.ForumManager.RemoveForumHook;
import org.libreproject.libre.api.forum.ForumSharingManager;

import javax.inject.Inject;

@NotNullByDefault
class ForumSharingManagerImpl extends SharingManagerImpl<Forum>
		implements ForumSharingManager, RemoveForumHook {

	@Inject
	ForumSharingManagerImpl(DatabaseComponent db, ClientHelper clientHelper,
			ClientVersioningManager clientVersioningManager,
			MetadataParser metadataParser, MessageParser<Forum> messageParser,
			SessionEncoder sessionEncoder, SessionParser sessionParser,
			MessageTracker messageTracker,
			ContactGroupFactory contactGroupFactory,
			ProtocolEngine<Forum> engine,
			InvitationFactory<Forum, ForumInvitationResponse> invitationFactory) {
		super(db, clientHelper, clientVersioningManager, metadataParser,
				messageParser, sessionEncoder, sessionParser, messageTracker,
				contactGroupFactory, engine, invitationFactory);
	}

	@Override
	protected ClientId getClientId() {
		return CLIENT_ID;
	}

	@Override
	protected int getMajorVersion() {
		return MAJOR_VERSION;
	}

	@Override
	protected ClientId getShareableClientId() {
		return ForumManager.CLIENT_ID;
	}

	@Override
	protected int getShareableMajorVersion() {
		return ForumManager.MAJOR_VERSION;
	}

	@Override
	public void removingForum(Transaction txn, Forum f) throws DbException {
		removingShareable(txn, f);
	}

}

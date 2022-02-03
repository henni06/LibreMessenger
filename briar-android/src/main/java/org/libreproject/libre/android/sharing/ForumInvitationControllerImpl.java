package org.libreproject.libre.android.sharing;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.db.DatabaseExecutor;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.ClientId;
import org.libreproject.libre.android.controller.handler.ExceptionHandler;
import org.libreproject.libre.api.forum.Forum;
import org.libreproject.libre.api.forum.ForumSharingManager;
import org.libreproject.libre.api.forum.event.ForumInvitationRequestReceivedEvent;
import org.libreproject.libre.api.sharing.SharingInvitationItem;

import java.util.Collection;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static org.libreproject.bramble.util.LogUtils.logException;
import static org.libreproject.libre.api.forum.ForumManager.CLIENT_ID;

@NotNullByDefault
class ForumInvitationControllerImpl
		extends InvitationControllerImpl<SharingInvitationItem>
		implements ForumInvitationController {

	private final ForumSharingManager forumSharingManager;

	@Inject
	ForumInvitationControllerImpl(@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager, EventBus eventBus,
			ForumSharingManager forumSharingManager) {
		super(dbExecutor, lifecycleManager, eventBus);
		this.forumSharingManager = forumSharingManager;
	}

	@Override
	public void eventOccurred(Event e) {
		super.eventOccurred(e);

		if (e instanceof ForumInvitationRequestReceivedEvent) {
			LOG.info("Forum invitation received, reloading");
			listener.loadInvitations(false);
		}
	}

	@Override
	protected ClientId getShareableClientId() {
		return CLIENT_ID;
	}

	@Override
	protected Collection<SharingInvitationItem> getInvitations()
			throws DbException {
		return forumSharingManager.getInvitations();
	}

	@Override
	public void respondToInvitation(SharingInvitationItem item, boolean accept,
			ExceptionHandler<DbException> handler) {
		runOnDbThread(() -> {
			try {
				Forum f = (Forum) item.getShareable();
				for (Contact c : item.getNewSharers()) {
					// TODO: What happens if a contact has been removed?
					forumSharingManager.respondToInvitation(f, c, accept);
				}
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				handler.onException(e);
			}
		});
	}

}

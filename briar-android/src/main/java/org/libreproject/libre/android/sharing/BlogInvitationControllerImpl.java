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
import org.libreproject.libre.api.blog.Blog;
import org.libreproject.libre.api.blog.BlogSharingManager;
import org.libreproject.libre.api.blog.event.BlogInvitationRequestReceivedEvent;
import org.libreproject.libre.api.sharing.SharingInvitationItem;

import java.util.Collection;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static org.libreproject.bramble.util.LogUtils.logException;
import static org.libreproject.libre.api.blog.BlogManager.CLIENT_ID;

@NotNullByDefault
class BlogInvitationControllerImpl
		extends InvitationControllerImpl<SharingInvitationItem>
		implements BlogInvitationController {

	private final BlogSharingManager blogSharingManager;

	@Inject
	BlogInvitationControllerImpl(@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager, EventBus eventBus,
			BlogSharingManager blogSharingManager) {
		super(dbExecutor, lifecycleManager, eventBus);
		this.blogSharingManager = blogSharingManager;
	}

	@Override
	public void eventOccurred(Event e) {
		super.eventOccurred(e);

		if (e instanceof BlogInvitationRequestReceivedEvent) {
			LOG.info("Blog invitation received, reloading");
			listener.loadInvitations(false);
		}
	}

	@Override
	protected ClientId getShareableClientId() {
		return CLIENT_ID;
	}

	@Override
	protected Collection<SharingInvitationItem> getInvitations() throws DbException {
		return blogSharingManager.getInvitations();
	}

	@Override
	public void respondToInvitation(SharingInvitationItem item, boolean accept,
			ExceptionHandler<DbException> handler) {
		runOnDbThread(() -> {
			try {
				Blog f = (Blog) item.getShareable();
				for (Contact c : item.getNewSharers()) {
					// TODO: What happens if a contact has been removed?
					blogSharingManager.respondToInvitation(f, c, accept);
				}
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				handler.onException(e);
			}
		});
	}

}

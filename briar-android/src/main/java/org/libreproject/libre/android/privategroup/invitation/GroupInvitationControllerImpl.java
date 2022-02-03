package org.libreproject.libre.android.privategroup.invitation;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.db.DatabaseExecutor;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.ClientId;
import org.libreproject.libre.android.controller.handler.ExceptionHandler;
import org.libreproject.libre.android.sharing.InvitationControllerImpl;
import org.libreproject.libre.api.privategroup.PrivateGroup;
import org.libreproject.libre.api.privategroup.event.GroupInvitationRequestReceivedEvent;
import org.libreproject.libre.api.privategroup.event.GroupInvitationResponseReceivedEvent;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationItem;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationManager;

import java.util.Collection;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static org.libreproject.bramble.util.LogUtils.logException;
import static org.libreproject.libre.api.privategroup.PrivateGroupManager.CLIENT_ID;

@NotNullByDefault
class GroupInvitationControllerImpl
		extends InvitationControllerImpl<GroupInvitationItem>
		implements GroupInvitationController {

	private final GroupInvitationManager groupInvitationManager;

	@Inject
	GroupInvitationControllerImpl(@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager, EventBus eventBus,
			GroupInvitationManager groupInvitationManager) {
		super(dbExecutor, lifecycleManager, eventBus);
		this.groupInvitationManager = groupInvitationManager;
	}

	@Override
	public void eventOccurred(Event e) {
		super.eventOccurred(e);

		if (e instanceof GroupInvitationRequestReceivedEvent) {
			LOG.info("Group invitation request received, reloading");
			listener.loadInvitations(false);
		} else if (e instanceof GroupInvitationResponseReceivedEvent) {
			LOG.info("Group invitation response received, reloading");
			listener.loadInvitations(false);
		}
	}

	@Override
	protected ClientId getShareableClientId() {
		return CLIENT_ID;
	}

	@Override
	protected Collection<GroupInvitationItem> getInvitations()
			throws DbException {
		return groupInvitationManager.getInvitations();
	}

	@Override
	public void respondToInvitation(GroupInvitationItem item, boolean accept,
			ExceptionHandler<DbException> handler) {
		runOnDbThread(() -> {
			try {
				PrivateGroup g = item.getShareable();
				ContactId c = item.getCreator().getId();
				groupInvitationManager.respondToInvitation(c, g, accept);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				handler.onException(e);
			}
		});
	}

}

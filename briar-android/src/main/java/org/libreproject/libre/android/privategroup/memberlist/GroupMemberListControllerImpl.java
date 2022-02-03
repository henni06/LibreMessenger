package org.libreproject.libre.android.privategroup.memberlist;

import org.libreproject.bramble.api.connection.ConnectionRegistry;
import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.db.DatabaseExecutor;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.android.controller.DbControllerImpl;
import org.libreproject.libre.android.controller.handler.ResultExceptionHandler;
import org.libreproject.libre.api.privategroup.GroupMember;
import org.libreproject.libre.api.privategroup.PrivateGroupManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static org.libreproject.bramble.util.LogUtils.logException;

class GroupMemberListControllerImpl extends DbControllerImpl
		implements GroupMemberListController {

	private static final Logger LOG =
			Logger.getLogger(GroupMemberListControllerImpl.class.getName());

	private final ConnectionRegistry connectionRegistry;
	private final PrivateGroupManager privateGroupManager;

	@Inject
	GroupMemberListControllerImpl(@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager,
			ConnectionRegistry connectionRegistry,
			PrivateGroupManager privateGroupManager) {
		super(dbExecutor, lifecycleManager);
		this.connectionRegistry = connectionRegistry;
		this.privateGroupManager = privateGroupManager;
	}

	@Override
	public void loadMembers(GroupId groupId,
			ResultExceptionHandler<Collection<MemberListItem>, DbException> handler) {
		runOnDbThread(() -> {
			try {
				Collection<MemberListItem> items = new ArrayList<>();
				Collection<GroupMember> members =
						privateGroupManager.getMembers(groupId);
				for (GroupMember m : members) {
					ContactId c = m.getContactId();
					boolean online = false;
					if (c != null)
						online = connectionRegistry.isConnected(c);
					items.add(new MemberListItem(m, online));
				}
				handler.onResult(items);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				handler.onException(e);
			}
		});
	}

}

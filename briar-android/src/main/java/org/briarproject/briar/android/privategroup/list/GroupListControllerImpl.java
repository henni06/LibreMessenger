package org.briarproject.briar.android.privategroup.list;

import android.support.annotation.CallSuper;

import org.briarproject.bramble.api.contact.ContactManager;
import org.briarproject.bramble.api.db.DatabaseExecutor;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.NoSuchGroupException;
import org.briarproject.bramble.api.event.Event;
import org.briarproject.bramble.api.event.EventBus;
import org.briarproject.bramble.api.event.EventListener;
import org.briarproject.bramble.api.identity.AuthorId;
import org.briarproject.bramble.api.identity.AuthorInfo;
import org.briarproject.bramble.api.lifecycle.LifecycleManager;
import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.bramble.api.sync.ClientId;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.event.GroupAddedEvent;
import org.briarproject.bramble.api.sync.event.GroupRemovedEvent;
import org.briarproject.briar.android.controller.DbControllerImpl;
import org.briarproject.briar.android.controller.handler.ExceptionHandler;
import org.briarproject.briar.android.controller.handler.ResultExceptionHandler;
import org.briarproject.briar.api.android.AndroidNotificationManager;
import org.briarproject.briar.api.client.MessageTracker.GroupCount;
import org.briarproject.briar.api.privategroup.PrivateGroup;
import org.briarproject.briar.api.privategroup.PrivateGroupManager;
import org.briarproject.briar.api.privategroup.event.GroupDissolvedEvent;
import org.briarproject.briar.api.privategroup.event.GroupInvitationRequestReceivedEvent;
import org.briarproject.briar.api.privategroup.event.GroupMessageAddedEvent;
import org.briarproject.briar.api.privategroup.invitation.GroupInvitationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static org.briarproject.bramble.util.LogUtils.logDuration;
import static org.briarproject.bramble.util.LogUtils.logException;
import static org.briarproject.bramble.util.LogUtils.now;
import static org.briarproject.briar.api.privategroup.PrivateGroupManager.CLIENT_ID;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
class GroupListControllerImpl extends DbControllerImpl
		implements GroupListController, EventListener {

	private static final Logger LOG =
			Logger.getLogger(GroupListControllerImpl.class.getName());

	private final PrivateGroupManager groupManager;
	private final GroupInvitationManager groupInvitationManager;
	private final ContactManager contactManager;
	private final AndroidNotificationManager notificationManager;
	private final EventBus eventBus;

	// UI thread
	private GroupListListener listener;

	@Inject
	GroupListControllerImpl(@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager, PrivateGroupManager groupManager,
			GroupInvitationManager groupInvitationManager,
			ContactManager contactManager,
			AndroidNotificationManager notificationManager, EventBus eventBus) {
		super(dbExecutor, lifecycleManager);
		this.groupManager = groupManager;
		this.groupInvitationManager = groupInvitationManager;
		this.contactManager = contactManager;
		this.notificationManager = notificationManager;
		this.eventBus = eventBus;
	}

	@Override
	public void setGroupListListener(GroupListListener listener) {
		this.listener = listener;
	}

	@Override
	@CallSuper
	public void onStart() {
		if (listener == null) throw new IllegalStateException();
		eventBus.addListener(this);
		notificationManager.clearAllGroupMessageNotifications();
	}

	@Override
	@CallSuper
	public void onStop() {
		eventBus.removeListener(this);
	}

	@Override
	@CallSuper
	public void eventOccurred(Event e) {
		if (e instanceof GroupMessageAddedEvent) {
			GroupMessageAddedEvent g = (GroupMessageAddedEvent) e;
			LOG.info("Private group message added");
			listener.onGroupMessageAdded(g.getHeader());
		} else if (e instanceof GroupInvitationRequestReceivedEvent) {
			LOG.info("Private group invitation received");
			listener.onGroupInvitationReceived();
		} else if (e instanceof GroupAddedEvent) {
			GroupAddedEvent g = (GroupAddedEvent) e;
			ClientId id = g.getGroup().getClientId();
			if (id.equals(CLIENT_ID)) {
				LOG.info("Private group added");
				listener.onGroupAdded(g.getGroup().getId());
			}
		} else if (e instanceof GroupRemovedEvent) {
			GroupRemovedEvent g = (GroupRemovedEvent) e;
			ClientId id = g.getGroup().getClientId();
			if (id.equals(CLIENT_ID)) {
				LOG.info("Private group removed");
				listener.onGroupRemoved(g.getGroup().getId());
			}
		} else if (e instanceof GroupDissolvedEvent) {
			GroupDissolvedEvent g = (GroupDissolvedEvent) e;
			LOG.info("Private group dissolved");
			listener.onGroupDissolved(g.getGroupId());
		}
	}

	@Override
	public void loadGroups(
			ResultExceptionHandler<Collection<GroupItem>, DbException> handler) {
		runOnDbThread(() -> {
			try {
				long start = now();
				Collection<PrivateGroup> groups =
						groupManager.getPrivateGroups();
				List<GroupItem> items = new ArrayList<>(groups.size());
				Map<AuthorId, AuthorInfo> authorInfos = new HashMap<>();
				for (PrivateGroup g : groups) {
					try {
						GroupId id = g.getId();
						AuthorId authorId = g.getCreator().getId();
						AuthorInfo authorInfo;
						if (authorInfos.containsKey(authorId)) {
							authorInfo = authorInfos.get(authorId);
						} else {
							authorInfo = contactManager.getAuthorInfo(authorId);
							authorInfos.put(authorId, authorInfo);
						}
						GroupCount count = groupManager.getGroupCount(id);
						boolean dissolved = groupManager.isDissolved(id);
						items.add(
								new GroupItem(g, authorInfo, count, dissolved));
					} catch (NoSuchGroupException e) {
						// Continue
					}
				}
				logDuration(LOG, "Loading groups", start);
				handler.onResult(items);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				handler.onException(e);
			}
		});
	}

	@Override
	public void removeGroup(GroupId g, ExceptionHandler<DbException> handler) {
		runOnDbThread(() -> {
			try {
				long start = now();
				groupManager.removePrivateGroup(g);
				logDuration(LOG, "Removing group", start);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				handler.onException(e);
			}
		});
	}

	@Override
	public void loadAvailableGroups(
			ResultExceptionHandler<Integer, DbException> handler) {
		runOnDbThread(() -> {
			try {
				handler.onResult(
						groupInvitationManager.getInvitations().size());
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				handler.onException(e);
			}
		});
	}

}

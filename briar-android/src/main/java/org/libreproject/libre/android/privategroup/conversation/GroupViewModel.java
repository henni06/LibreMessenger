package org.libreproject.libre.android.privategroup.conversation;

import android.app.Application;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.crypto.CryptoExecutor;
import org.libreproject.bramble.api.db.DatabaseExecutor;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.db.TransactionManager;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.identity.IdentityManager;
import org.libreproject.bramble.api.identity.LocalAuthor;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.api.system.AndroidExecutor;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.libre.android.sharing.SharingController;
import org.libreproject.libre.android.threaded.ThreadListViewModel;
import org.libreproject.libre.android.threaded.ThreadMap;
import org.libreproject.libre.api.android.AndroidNotificationManager;
import org.libreproject.libre.api.client.MessageTracker;
import org.libreproject.libre.api.client.MessageTracker.GroupCount;
import org.libreproject.libre.api.identity.AuthorManager;
import org.libreproject.libre.api.privategroup.GroupMember;
import org.libreproject.libre.api.privategroup.GroupMessage;
import org.libreproject.libre.api.privategroup.GroupMessageFactory;
import org.libreproject.libre.api.privategroup.GroupMessageHeader;
import org.libreproject.libre.api.privategroup.JoinMessageHeader;
import org.libreproject.libre.api.privategroup.PrivateGroup;
import org.libreproject.libre.api.privategroup.PrivateGroupManager;
import org.libreproject.libre.api.privategroup.event.ContactRelationshipRevealedEvent;
import org.libreproject.libre.api.privategroup.event.GroupDissolvedEvent;
import org.libreproject.libre.api.privategroup.event.GroupInvitationResponseReceivedEvent;
import org.libreproject.libre.api.privategroup.event.GroupMessageAddedEvent;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static java.lang.Math.max;
import static java.util.logging.Logger.getLogger;
import static org.libreproject.bramble.util.LogUtils.logDuration;
import static org.libreproject.bramble.util.LogUtils.now;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
class GroupViewModel extends ThreadListViewModel<GroupMessageItem> {

	private static final Logger LOG = getLogger(GroupViewModel.class.getName());

	private final PrivateGroupManager privateGroupManager;
	private final GroupMessageFactory groupMessageFactory;


	private final MutableLiveData<PrivateGroup> privateGroup =
			new MutableLiveData<>();
	private final MutableLiveData<Boolean> isCreator = new MutableLiveData<>();
	private final MutableLiveData<Boolean> isDissolved =
			new MutableLiveData<>();




	@Inject
	GroupViewModel(Application application,
			@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager,
			TransactionManager db,
			AndroidExecutor androidExecutor,
			EventBus eventBus,
			IdentityManager identityManager,
			AndroidNotificationManager notificationManager,
			SharingController sharingController,
			@CryptoExecutor Executor cryptoExecutor,
			Clock clock,
			MessageTracker messageTracker,
			PrivateGroupManager privateGroupManager,
			GroupMessageFactory groupMessageFactory, AuthorManager authorManager,
			ContactManager contactManager) {
		super(application, dbExecutor, lifecycleManager, db, androidExecutor,
				identityManager, notificationManager, sharingController,
				cryptoExecutor, clock, messageTracker, eventBus,authorManager,contactManager);
		this.privateGroupManager = privateGroupManager;
		this.groupMessageFactory = groupMessageFactory;
	}


	@Override
	public void eventOccurred(Event e) {

		if (e instanceof GroupMessageAddedEvent) {
			GroupMessageAddedEvent g = (GroupMessageAddedEvent) e;
			// only act on non-local messages in this group
			if (!g.isLocal() && g.getGroupId().equals(groupId)) {
				LOG.info("Group message received, adding...");
				GroupMessageItem item = buildItem(g.getHeader(), g.getText());
				if(item.getText().startsWith(ThreadMap.MARKER_IDENTIFIER)){
					getThreadMap().handleMarkerMessage(item);
				}else
				if(item.getText().startsWith(ThreadMap.LOCATION_IDENTIFIER)){
					try {

						getThreadMap().handleLocationMessage(item);
					}
					catch(Exception ex){
						ex.printStackTrace();
					}

				}else {
					addItem(item, false);
				}
				// In case the join message comes from the creator,
				// we need to reload the sharing contacts
				// in case it was delayed and the sharing count is wrong (#850).
				if (item instanceof JoinMessageItem &&
						(((JoinMessageItem) item).isInitial())) {
					loadSharingContacts();
				}
			}
		} else if (e instanceof GroupInvitationResponseReceivedEvent) {
			GroupInvitationResponseReceivedEvent g =
					(GroupInvitationResponseReceivedEvent) e;
			GroupInvitationResponse r = g.getMessageHeader();
			if (r.getShareableId().equals(groupId) && r.wasAccepted()) {
				sharingController.add(g.getContactId());
			}
		} else if (e instanceof ContactRelationshipRevealedEvent) {
			ContactRelationshipRevealedEvent c =
					(ContactRelationshipRevealedEvent) e;
			if (c.getGroupId().equals(groupId)) {
				sharingController.add(c.getContactId());
			}
		} else if (e instanceof GroupDissolvedEvent) {
			GroupDissolvedEvent g = (GroupDissolvedEvent) e;
			if (g.getGroupId().equals(groupId)) {
				isDissolved.setValue(true);
			}
		} else {
			super.eventOccurred(e);
		}
	}

	@Override
	protected void performInitialLoad() {
		super.performInitialLoad();
		loadPrivateGroup(groupId);
	}

	@Override
	protected void clearNotifications() {
		notificationManager.clearGroupMessageNotification(groupId);
	}

	private void loadPrivateGroup(GroupId groupId) {
		runOnDbThread(() -> {
			try {
				PrivateGroup g = privateGroupManager.getPrivateGroup(groupId);
				privateGroup.postValue(g);
				Author author = identityManager.getLocalAuthor();
				isCreator.postValue(g.getCreator().equals(author));
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	@Override
	public void loadItems() {
		loadFromDb(txn -> {
			// check first if group is dissolved
			isDissolved
					.postValue(privateGroupManager.isDissolved(txn, groupId));
			// now continue to load the items
			long start = now();
			List<GroupMessageHeader> headers =
					privateGroupManager.getHeaders(txn, groupId);
			logDuration(LOG, "Loading headers", start);
			start = now();
			List<GroupMessageItem> items = new ArrayList<>();
			for (GroupMessageHeader header : headers) {
				try {
					GroupMessageItem item = loadItem(txn, header);
					if (item.getText()
							.startsWith(ThreadMap.MARKER_IDENTIFIER)) {
						getThreadMap().handleMarkerMessage(item);
					} else if (
							item.getText().startsWith(
									ThreadMap.LOCATION_IDENTIFIER)) {

					} else {
						items.add(item);
					}
				}
				catch (Exception e){}

			}
			logDuration(LOG, "Loading bodies and creating items", start);
			return items;
		}, this::setItems);
	}

	private GroupMessageItem loadItem(Transaction txn,
			GroupMessageHeader header) throws DbException {
		String text;
		if (header instanceof JoinMessageHeader) {
			// will be looked up later
			text = "";
		} else {
			text = privateGroupManager.getMessageText(txn, header.getId());
		}
		return buildItem(header, text);
	}

	private GroupMessageItem buildItem(GroupMessageHeader header, String text) {
		if (header instanceof JoinMessageHeader) {
			return new JoinMessageItem((JoinMessageHeader) header, text);
		}
		return new GroupMessageItem(header, text);
	}

	@Override
	public void createAndStoreMessage(String text,
			@Nullable MessageId parentId) {
		runOnDbThread(() -> {
			try {
				LocalAuthor author = identityManager.getLocalAuthor();
				MessageId previousMsgId =
						privateGroupManager.getPreviousMsgId(groupId);
				GroupCount count = privateGroupManager.getGroupCount(groupId);
				long timestamp = count.getLatestMsgTime();
				timestamp = max(clock.currentTimeMillis(), timestamp + 1);
				createMessage(text, timestamp, parentId, author, previousMsgId);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}


	@Override
	public void createAndStoreLocationMessage(String text,
			@Nullable MessageId parentId) {
		runOnDbThread(() -> {
			try {
				LocalAuthor author = identityManager.getLocalAuthor();
				MessageId previousMsgId =
						privateGroupManager.getPreviousMsgId(groupId);
				GroupCount count = privateGroupManager.getGroupCount(groupId);
				long timestamp = count.getLatestMsgTime();
				timestamp = max(clock.currentTimeMillis(), timestamp + 1);
				createLocationMessage(text, timestamp, parentId, author, previousMsgId);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	@Override
	public void createAndStoreMarkerMessage(String text,
			@Nullable MessageId parentId) {
		runOnDbThread(() -> {
			try {
				LocalAuthor author = identityManager.getLocalAuthor();
				MessageId previousMsgId =
						privateGroupManager.getPreviousMsgId(groupId);
				GroupCount count = privateGroupManager.getGroupCount(groupId);
				long timestamp = count.getLatestMsgTime();
				timestamp = max(clock.currentTimeMillis(), timestamp + 1);
				createMarkerMessage(text, timestamp, parentId, author, previousMsgId);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}


	private void createMessage(String text, long timestamp,
			@Nullable MessageId parentId, LocalAuthor author,
			MessageId previousMsgId) {
		cryptoExecutor.execute(() -> {
			LOG.info("Creating group message...");
			GroupMessage msg = groupMessageFactory.createGroupMessage(groupId,
					timestamp, parentId, author, text, previousMsgId,
					Message.MessageType.DEFAULT);
			storePost(msg, text);
		});
	}

	private void createLocationMessage(String text, long timestamp,
			@Nullable MessageId parentId, LocalAuthor author,
			MessageId previousMsgId) {
		cryptoExecutor.execute(() -> {
			LOG.info("Creating location message...");
			GroupMessage msg = groupMessageFactory.createGroupMessage(groupId,
					timestamp, parentId, author, text, previousMsgId,
					Message.MessageType.LOCATION);
			storeLocation(msg);
		});
	}

	private void createMarkerMessage(String text, long timestamp,
			@Nullable MessageId parentId, LocalAuthor author,
			MessageId previousMsgId) {
		cryptoExecutor.execute(() -> {
			LOG.info("Creating marker message...");
			GroupMessage msg = groupMessageFactory.createGroupMessage(groupId,
					timestamp, parentId, author, text, previousMsgId,
					Message.MessageType.MARKER);
			storeMarker(msg);
		});
	}



	private void storePost(GroupMessage msg, String text) {
		runOnDbThread(false, txn -> {
			long start = now();
			GroupMessageHeader header =
					privateGroupManager.addLocalMessage(txn, msg);
			logDuration(LOG, "Storing group message", start);
			txn.attach(() ->
					addItem(buildItem(header, text), true)
			);
		}, this::handleException);
	}

	private void storeLocation(GroupMessage msg) {
		runOnDbThread(false, txn -> {
			long start = now();
			GroupMessageHeader header =
					privateGroupManager.addLocalMessage(txn, msg);
			logDuration(LOG, "Storing location message", start);
			txn.attach(new Runnable() {
				@Override
				public void run() {

				}
			});
		}, this::handleException);
	}

	private void storeMarker(GroupMessage msg) {
		runOnDbThread(false, txn -> {
			long start = now();
			GroupMessageHeader header =
					privateGroupManager.addLocalMessage(txn, msg);
			logDuration(LOG, "Storing location message", start);
			txn.attach(new Runnable() {
				@Override
				public void run() {

				}
			});
		}, this::handleException);
	}

	@Override
	protected void markItemRead(GroupMessageItem item) {
		runOnDbThread(() -> {
			try {
				privateGroupManager.setReadFlag(groupId, item.getId(), true);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	@Override
	public void loadSharingContacts() {
		runOnDbThread(true, txn -> {
			Collection<GroupMember> members =
					privateGroupManager.getMembers(txn, groupId);
			Collection<ContactId> contactIds = new ArrayList<>();
			for (GroupMember m : members) {
				if (m.getContactId() != null)
					contactIds.add(m.getContactId());
			}
			txn.attach(() -> sharingController.addAll(contactIds));
		}, this::handleException);
	}

	void deletePrivateGroup() {
		runOnDbThread(() -> {
			try {
				privateGroupManager.removePrivateGroup(groupId);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	LiveData<PrivateGroup> getPrivateGroup() {
		return privateGroup;
	}

	protected LiveData<Boolean> isCreator() {
		return isCreator;
	}

	LiveData<Boolean> isDissolved() {
		return isDissolved;
	}

}

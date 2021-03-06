package org.libreproject.libre.android.threaded;

import android.app.Application;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.crypto.CryptoExecutor;
import org.libreproject.bramble.api.db.DatabaseExecutor;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.NoSuchGroupException;
import org.libreproject.bramble.api.db.TransactionManager;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.event.EventListener;
import org.libreproject.bramble.api.identity.IdentityManager;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.location.event.MarkerAddedEvent;
import org.libreproject.bramble.api.location.event.MarkerRemovedEvent;
import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.bramble.api.sync.event.GroupRemovedEvent;
import org.libreproject.bramble.api.sync.event.LocationMessageEvent;
import org.libreproject.bramble.api.system.AndroidExecutor;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.libre.android.sharing.SharingController;
import org.libreproject.libre.android.sharing.SharingController.SharingInfo;
import org.libreproject.libre.android.viewmodel.DbViewModel;
import org.libreproject.libre.android.viewmodel.LiveResult;
import org.libreproject.libre.api.android.AndroidNotificationManager;
import org.libreproject.libre.api.client.MessageTracker;
import org.libreproject.libre.api.client.MessageTree;
import org.libreproject.libre.api.identity.AuthorManager;
import org.libreproject.libre.client.MessageTreeImpl;
import org.osmdroid.util.GeoPoint;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import androidx.annotation.CallSuper;
import androidx.annotation.UiThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.INFO;
import static java.util.logging.Logger.getLogger;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public abstract class ThreadListViewModel<I extends ThreadItem>
		extends DbViewModel implements EventListener {

	private static final Logger LOG =
			getLogger(ThreadListViewModel.class.getName());

	protected final IdentityManager identityManager;
	protected final AndroidNotificationManager notificationManager;
	protected final SharingController sharingController;
	protected final Executor cryptoExecutor;
	protected final Clock clock;
	private final MessageTracker messageTracker;
	private final EventBus eventBus;
	private final AuthorManager authorManager;
	private final ContactManager contactManager;
	// UIThread
	private final MessageTree<I> messageTree = new MessageTreeImpl<>();
	private final MutableLiveData<LiveResult<List<I>>> items =
			new MutableLiveData<>();
	private final MutableLiveData<LiveResult<List<GeoPoint>>> locations =
			new MutableLiveData<>();

	private final MutableLiveData<Boolean> groupRemoved =
			new MutableLiveData<>();
	private final AtomicReference<MessageId> scrollToItem =
			new AtomicReference<>();

	private ThreadMap threadMap;

	public ThreadMap getThreadMap() {
		return threadMap;
	}

	public void setThreadMap(ThreadMap threadMap) {
		this.threadMap = threadMap;
	}

	protected volatile GroupId groupId;
	@Nullable
	private MessageId replyId;
	/**
	 * Stored list position. Needs to be loaded and set before the list itself.
	 */
	private final AtomicReference<MessageId> storedMessageId =
			new AtomicReference<>();

	public ThreadListViewModel(Application application,
			@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager,
			TransactionManager db,
			AndroidExecutor androidExecutor,
			IdentityManager identityManager,
			AndroidNotificationManager notificationManager,
			SharingController sharingController,
			@CryptoExecutor Executor cryptoExecutor,
			Clock clock,
			MessageTracker messageTracker,
			EventBus eventBus,AuthorManager authorManager,ContactManager contactManager) {
		super(application, dbExecutor, lifecycleManager, db, androidExecutor);
		this.identityManager = identityManager;
		this.contactManager=contactManager;
		this.notificationManager = notificationManager;
		this.cryptoExecutor = cryptoExecutor;
		this.clock = clock;
		this.sharingController = sharingController;
		this.messageTracker = messageTracker;
		this.eventBus = eventBus;
		this.eventBus.addListener(this);
		this.authorManager=authorManager;
	}

	@Override
	protected void onCleared() {
		super.onCleared();
		eventBus.removeListener(this);
		sharingController.onCleared();
	}

	/**
	 * Needs to be called right after initialization,
	 * before calling any other methods.
	 */
	public final void setGroupId(GroupId groupId) {
		boolean needsInitialLoad = this.groupId == null;
		this.groupId = groupId;
		if (needsInitialLoad) performInitialLoad();
	}

	@CallSuper
	protected void performInitialLoad() {
		// load stored MessageId (last list position) before the list itself
		loadStoredMessageId();
		loadItems();
		loadSharingContacts();
	}

	protected abstract void clearNotifications();

	void blockAndClearNotifications() {
		notificationManager.blockNotification(groupId);
		clearNotifications();
	}

	void unblockNotifications() {
		notificationManager.unblockNotification(groupId);
	}

	@Override
	@CallSuper
	public void eventOccurred(Event e) {
		if(e instanceof MarkerAddedEvent){
			MarkerAddedEvent mae=(MarkerAddedEvent)e;
			threadMap.onMarkerAdded(mae);

		}else
		if(e instanceof MarkerRemovedEvent){
			MarkerRemovedEvent mre=(MarkerRemovedEvent)e;
			threadMap.onMarkerRemoved(mre);

		}else
		if (e instanceof GroupRemovedEvent) {
			GroupRemovedEvent s = (GroupRemovedEvent) e;
			if (s.getGroup().getId().equals(groupId)) {
				LOG.info("Group removed");
				groupRemoved.setValue(true);
			}
		}else if(e instanceof LocationMessageEvent){

			LocationMessageEvent g = (LocationMessageEvent) e;
			try{
				Contact contact=contactManager.getContact(g.getContactId());

				getThreadMap().handleLocationMessage(g,contact.getAuthor(),contact.getAlias());
			} catch (Exception exception) {
				exception.printStackTrace();
			}

		}
	}

	private void loadStoredMessageId() {
		runOnDbThread(() -> {
			try {
				storedMessageId
						.set(messageTracker.loadStoredMessageId(groupId));
				if (LOG.isLoggable(INFO)) {
					LOG.info("Loaded last top visible message id " +
							storedMessageId);
				}
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	public abstract void loadItems();

	public abstract void createAndStoreMessage(String text,
			@Nullable MessageId parentMessageId);

	public abstract void createAndStoreLocationMessage(String text,
			@Nullable MessageId parentMessageId);

	public abstract void createAndStoreMarkerMessage(String text,
			@Nullable MessageId parentMessageId);
	/**
	 * Loads the ContactIds of all contacts the group is shared with
	 * and adds them to {@link SharingController}.
	 */
	protected abstract void loadSharingContacts();

	@UiThread
	protected void addLocation(GeoPoint point) {
		List<GeoPoint>locationLD=locations.getValue().getResultOrNull();
		locationLD.add(point);
		LiveResult<List<GeoPoint>> liveData=new LiveResult<>(locationLD);
		locations.setValue(liveData);
	}

	@UiThread
	protected void setItems(LiveResult<List<I>> items) {
		if (items.hasError()) {
			this.items.setValue(items);
		} else {
			messageTree.clear();
			// not null, because hasError() is false
			messageTree.add(requireNonNull(items.getResultOrNull()));
			LiveResult<List<I>> result =
					new LiveResult<>(messageTree.depthFirstOrder());
			this.items.setValue(result);
		}
	}

	/**
	 * Add a remote item on the UI thread.
	 *
	 * @param scrollToItem whether the list will scroll to the newly added item
	 */
	@UiThread
	protected void addItem(I item, boolean scrollToItem) {
		// If items haven't loaded, we need to wait until they have.
		// Since this was a R/W DB transaction, the load will pick up this item.
		if(!item.getText().startsWith(Message.LOCATION_IDENTIFIER) &&
		!item.getText().startsWith(Message.MARKER_IDENTIFIER)) {
			if (items.getValue() == null) return;
			messageTree.add(item);
			if (scrollToItem) this.scrollToItem.set(item.getId());
			items.setValue(new LiveResult<>(messageTree.depthFirstOrder()));
		}
	}

	@UiThread
	void setReplyId(@Nullable MessageId id) {
		replyId = id;
	}

	@UiThread
	@Nullable
	MessageId getReplyId() {
		return replyId;
	}

	@UiThread
	@Nullable
	LiveData<LiveResult<List<GeoPoint>>> getLocations(){ return locations;}

	@UiThread
	void storeMessageId(@Nullable MessageId messageId) {
		if (messageId != null) {
			runOnDbThread(() -> {
				try {
					messageTracker.storeMessageId(groupId, messageId);
				} catch (NoSuchGroupException e) {
					// This can happen when the activity is closed
					// after deleting the group. So just ignore this case.
				} catch (DbException e) {
					handleException(e);
				}
			});
		}
	}

	protected abstract void markItemRead(I item);

	/**
	 * Returns the {@link MessageId} of the item that was at the top of the
	 * list last time or null if there has been nothing stored, yet.
	 */
	@Nullable
	MessageId getAndResetRestoredMessageId() {
		return storedMessageId.getAndSet(null);
	}

	LiveData<LiveResult<List<I>>> getItems() {
		return items;
	}

	public LiveData<SharingInfo> getSharingInfo() {
		return sharingController.getSharingInfo();
	}

	LiveData<Boolean> getGroupRemoved() {
		return groupRemoved;
	}

	@Nullable
	MessageId getAndResetScrollToItem() {
		return scrollToItem.getAndSet(null);
	}

	protected abstract LiveData<Boolean> isCreator();

}

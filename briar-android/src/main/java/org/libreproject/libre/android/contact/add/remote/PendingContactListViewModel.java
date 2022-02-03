package org.libreproject.libre.android.contact.add.remote;

import android.app.Application;

import org.libreproject.bramble.api.Pair;
import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.contact.PendingContact;
import org.libreproject.bramble.api.contact.PendingContactId;
import org.libreproject.bramble.api.contact.PendingContactState;
import org.libreproject.bramble.api.contact.event.PendingContactRemovedEvent;
import org.libreproject.bramble.api.contact.event.PendingContactStateChangedEvent;
import org.libreproject.bramble.api.db.DatabaseExecutor;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.TransactionManager;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.event.EventListener;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.rendezvous.RendezvousPoller;
import org.libreproject.bramble.api.rendezvous.event.RendezvousPollEvent;
import org.libreproject.bramble.api.system.AndroidExecutor;
import org.libreproject.libre.android.viewmodel.DbViewModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static org.libreproject.bramble.api.contact.PendingContactState.OFFLINE;

@NotNullByDefault
public class PendingContactListViewModel extends DbViewModel
		implements EventListener {

	private final ContactManager contactManager;
	private final RendezvousPoller rendezvousPoller;
	private final EventBus eventBus;

	private final MutableLiveData<Collection<PendingContactItem>>
			pendingContacts = new MutableLiveData<>();
	private final MutableLiveData<Boolean> hasInternetConnection =
			new MutableLiveData<>();

	@Inject
	PendingContactListViewModel(Application application,
			@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager,
			TransactionManager db,
			AndroidExecutor androidExecutor,
			ContactManager contactManager,
			RendezvousPoller rendezvousPoller,
			EventBus eventBus) {
		super(application, dbExecutor, lifecycleManager, db, androidExecutor);
		this.contactManager = contactManager;
		this.rendezvousPoller = rendezvousPoller;
		this.eventBus = eventBus;
		this.eventBus.addListener(this);
	}

	void onCreate() {
		if (pendingContacts.getValue() == null) loadPendingContacts();
	}

	@Override
	protected void onCleared() {
		super.onCleared();
		eventBus.removeListener(this);
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof PendingContactStateChangedEvent ||
				e instanceof PendingContactRemovedEvent ||
				e instanceof RendezvousPollEvent) {
			loadPendingContacts();
		}
	}

	private void loadPendingContacts() {
		runOnDbThread(() -> {
			try {
				Collection<Pair<PendingContact, PendingContactState>> pairs =
						contactManager.getPendingContacts();
				List<PendingContactItem> items = new ArrayList<>(pairs.size());
				boolean online = pairs.isEmpty();
				for (Pair<PendingContact, PendingContactState> pair : pairs) {
					PendingContact p = pair.getFirst();
					PendingContactState state = pair.getSecond();
					long lastPoll = rendezvousPoller.getLastPollTime(p.getId());
					items.add(new PendingContactItem(p, state, lastPoll));
					online = online || state != OFFLINE;
				}
				pendingContacts.postValue(items);
				hasInternetConnection.postValue(online);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	LiveData<Collection<PendingContactItem>> getPendingContacts() {
		return pendingContacts;
	}

	void removePendingContact(PendingContactId id) {
		runOnDbThread(() -> {
			try {
				contactManager.removePendingContact(id);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	LiveData<Boolean> getHasInternetConnection() {
		return hasInternetConnection;
	}

}

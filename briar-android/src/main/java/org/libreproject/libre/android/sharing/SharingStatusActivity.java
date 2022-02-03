package org.libreproject.libre.android.sharing;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import org.libreproject.bramble.api.connection.ConnectionRegistry;
import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.db.DatabaseExecutor;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.event.EventListener;
import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.event.GroupRemovedEvent;
import org.libreproject.libre.R;
import org.libreproject.libre.android.activity.BriarActivity;
import org.libreproject.libre.android.contact.ContactItem;
import org.libreproject.libre.android.view.BriarRecyclerView;
import org.libreproject.libre.api.identity.AuthorInfo;
import org.libreproject.libre.api.identity.AuthorManager;
import org.libreproject.libre.api.sharing.event.ContactLeftShareableEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.annotation.CallSuper;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;

import static java.util.logging.Level.WARNING;
import static org.libreproject.bramble.util.LogUtils.logException;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
abstract class SharingStatusActivity extends BriarActivity
		implements EventListener {

	// objects accessed from background thread need to be volatile
	@Inject
	volatile AuthorManager authorManager;
	@Inject
	volatile ConnectionRegistry connectionRegistry;

	@Inject
	EventBus eventBus;

	private static final Logger LOG =
			Logger.getLogger(SharingStatusActivity.class.getName());

	private GroupId groupId;
	private BriarRecyclerView list;
	private SharingStatusAdapter adapter;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_sharing_status);

		Intent i = getIntent();
		byte[] b = i.getByteArrayExtra(GROUP_ID);
		if (b == null) throw new IllegalStateException("No GroupId");
		groupId = new GroupId(b);

		list = findViewById(R.id.list);
		adapter = new SharingStatusAdapter(this);
		list.setLayoutManager(new LinearLayoutManager(this));
		list.setAdapter(adapter);
		list.setEmptyText(getString(R.string.nobody));

		TextView info = findViewById(R.id.info);
		info.setText(getInfoText());
	}

	@Override
	public void onStart() {
		super.onStart();
		eventBus.addListener(this);
		loadSharedWith();
	}

	@Override
	public void onStop() {
		super.onStop();
		adapter.clear();
		eventBus.removeListener(this);
		list.showProgressBar();
	}

	@Override
	@CallSuper
	public void eventOccurred(Event e) {
		if (e instanceof ContactLeftShareableEvent) {
			ContactLeftShareableEvent c = (ContactLeftShareableEvent) e;
			if (c.getGroupId().equals(getGroupId())) {
				loadSharedWith();
			}
		} else if (e instanceof GroupRemovedEvent) {
			GroupRemovedEvent g = (GroupRemovedEvent) e;
			if (g.getGroup().getId().equals(getGroupId())) {
				supportFinishAfterTransition();
			}
		}
		// TODO ContactConnectedEvent and ContactDisconnectedEvent
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@StringRes
	abstract int getInfoText();

	@DatabaseExecutor
	abstract protected Collection<Contact> getSharedWith() throws DbException;

	protected GroupId getGroupId() {
		return groupId;
	}

	protected void loadSharedWith() {
		runOnDbThread(() -> {
			try {
				List<ContactItem> contactItems = new ArrayList<>();
				for (Contact c : getSharedWith()) {
					AuthorInfo authorInfo = authorManager.getAuthorInfo(c);
					boolean online = connectionRegistry.isConnected(c.getId());
					ContactItem item = new ContactItem(c, authorInfo, online);
					contactItems.add(item);
				}
				displaySharedWith(contactItems);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	private void displaySharedWith(List<ContactItem> contacts) {
		runOnUiThreadUnlessDestroyed(() -> {
			adapter.clear();
			if (contacts.isEmpty()) list.showData();
			else adapter.addAll(contacts);
		});
	}

}

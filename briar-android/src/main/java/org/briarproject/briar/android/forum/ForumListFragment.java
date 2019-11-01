package org.briarproject.briar.android.forum;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.UiThread;
import com.google.android.material.snackbar.Snackbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.briarproject.bramble.api.contact.event.ContactRemovedEvent;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.NoSuchGroupException;
import org.briarproject.bramble.api.event.Event;
import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.event.GroupAddedEvent;
import org.briarproject.bramble.api.sync.event.GroupRemovedEvent;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.fragment.BaseEventFragment;
import org.briarproject.briar.android.sharing.ForumInvitationActivity;
import org.briarproject.briar.android.util.BriarSnackbarBuilder;
import org.briarproject.briar.android.view.BriarRecyclerView;
import org.briarproject.briar.api.android.AndroidNotificationManager;
import org.briarproject.briar.api.client.MessageTracker.GroupCount;
import org.briarproject.briar.api.forum.Forum;
import org.briarproject.briar.api.forum.ForumManager;
import org.briarproject.briar.api.forum.ForumPostHeader;
import org.briarproject.briar.api.forum.ForumSharingManager;
import org.briarproject.briar.api.forum.event.ForumInvitationRequestReceivedEvent;
import org.briarproject.briar.api.forum.event.ForumPostReceivedEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.WARNING;
import static org.briarproject.bramble.util.LogUtils.logDuration;
import static org.briarproject.bramble.util.LogUtils.logException;
import static org.briarproject.bramble.util.LogUtils.now;
import static org.briarproject.briar.api.forum.ForumManager.CLIENT_ID;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class ForumListFragment extends BaseEventFragment implements
		OnClickListener {

	public final static String TAG = ForumListFragment.class.getName();
	private final static Logger LOG = Logger.getLogger(TAG);

	private BriarRecyclerView list;
	private ForumListAdapter adapter;
	private Snackbar snackbar;

	@Inject
	AndroidNotificationManager notificationManager;

	// Fields that are accessed from background threads must be volatile
	@Inject
	volatile ForumManager forumManager;
	@Inject
	volatile ForumSharingManager forumSharingManager;

	public static ForumListFragment newInstance() {

		Bundle args = new Bundle();

		ForumListFragment fragment = new ForumListFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void injectFragment(ActivityComponent component) {
		component.inject(this);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {

		requireNonNull(getActivity()).setTitle(R.string.forums_button);

		View contentView =
				inflater.inflate(R.layout.fragment_forum_list, container,
						false);

		adapter = new ForumListAdapter(getActivity());

		list = contentView.findViewById(R.id.forumList);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		list.setAdapter(adapter);

		snackbar = new BriarSnackbarBuilder()
				.setAction(R.string.show, this)
				.make(list, "", LENGTH_INDEFINITE);

		return contentView;
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

	@Override
	public void onStart() {
		super.onStart();
		notificationManager.clearAllForumPostNotifications();
		loadForums();
		loadAvailableForums();
		list.startPeriodicUpdate();
	}

	@Override
	public void onStop() {
		super.onStop();
		adapter.clear();
		list.showProgressBar();
		list.stopPeriodicUpdate();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.forum_list_actions, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_create_forum:
				Intent intent =
						new Intent(getContext(), CreateForumActivity.class);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void loadForums() {
		int revision = adapter.getRevision();
		listener.runOnDbThread(() -> {
			try {
				long start = now();
				Collection<ForumListItem> forums = new ArrayList<>();
				for (Forum f : forumManager.getForums()) {
					try {
						GroupCount count =
								forumManager.getGroupCount(f.getId());
						forums.add(new ForumListItem(f, count));
					} catch (NoSuchGroupException e) {
						// Continue
					}
				}
				logDuration(LOG, "Full load", start);
				displayForums(revision, forums);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	private void displayForums(int revision, Collection<ForumListItem> forums) {
		runOnUiThreadUnlessDestroyed(() -> {
			if (revision == adapter.getRevision()) {
				adapter.incrementRevision();
				if (forums.isEmpty()) list.showData();
				else adapter.replaceAll(forums);
			} else {
				LOG.info("Concurrent update, reloading");
				loadForums();
			}
		});
	}

	private void loadAvailableForums() {
		listener.runOnDbThread(() -> {
			try {
				long start = now();
				int available = forumSharingManager.getInvitations().size();
				logDuration(LOG, "Loading available", start);
				displayAvailableForums(available);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	private void displayAvailableForums(int availableCount) {
		runOnUiThreadUnlessDestroyed(() -> {
			if (availableCount == 0) {
				snackbar.dismiss();
			} else {
				snackbar.setText(getResources().getQuantityString(
						R.plurals.forums_shared, availableCount,
						availableCount));
				if (!snackbar.isShownOrQueued()) snackbar.show();
			}
		});
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof ContactRemovedEvent) {
			LOG.info("Contact removed, reloading available forums");
			loadAvailableForums();
		} else if (e instanceof GroupAddedEvent) {
			GroupAddedEvent g = (GroupAddedEvent) e;
			if (g.getGroup().getClientId().equals(CLIENT_ID)) {
				LOG.info("Forum added, reloading forums");
				loadForums();
			}
		} else if (e instanceof GroupRemovedEvent) {
			GroupRemovedEvent g = (GroupRemovedEvent) e;
			if (g.getGroup().getClientId().equals(CLIENT_ID)) {
				LOG.info("Forum removed, removing from list");
				removeForum(g.getGroup().getId());
			}
		} else if (e instanceof ForumPostReceivedEvent) {
			ForumPostReceivedEvent f = (ForumPostReceivedEvent) e;
			LOG.info("Forum post added, updating item");
			updateItem(f.getGroupId(), f.getHeader());
		} else if (e instanceof ForumInvitationRequestReceivedEvent) {
			LOG.info("Forum invitation received, reloading available forums");
			loadAvailableForums();
		}
	}

	@UiThread
	private void updateItem(GroupId g, ForumPostHeader m) {
		adapter.incrementRevision();
		int position = adapter.findItemPosition(g);
		ForumListItem item = adapter.getItemAt(position);
		if (item != null) {
			item.addHeader(m);
			adapter.updateItemAt(position, item);
		}
	}

	@UiThread
	private void removeForum(GroupId g) {
		adapter.incrementRevision();
		int position = adapter.findItemPosition(g);
		ForumListItem item = adapter.getItemAt(position);
		if (item != null) adapter.remove(item);
	}

	@Override
	public void onClick(View view) {
		// snackbar click
		Intent i = new Intent(getContext(), ForumInvitationActivity.class);
		startActivity(i);
	}
}

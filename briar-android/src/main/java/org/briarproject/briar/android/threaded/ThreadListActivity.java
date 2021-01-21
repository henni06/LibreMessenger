package org.briarproject.briar.android.threaded;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.MenuItem;

import com.google.android.material.snackbar.Snackbar;

import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.BriarActivity;
import org.briarproject.briar.android.controller.SharingController;
import org.briarproject.briar.android.controller.SharingController.SharingListener;
import org.briarproject.briar.android.controller.handler.UiResultExceptionHandler;
import org.briarproject.briar.android.threaded.ThreadItemAdapter.ThreadItemListener;
import org.briarproject.briar.android.threaded.ThreadListController.ThreadListDataSource;
import org.briarproject.briar.android.threaded.ThreadListController.ThreadListListener;
import org.briarproject.briar.android.util.BriarSnackbarBuilder;
import org.briarproject.briar.android.view.BriarRecyclerView;
import org.briarproject.briar.android.view.TextInputView;
import org.briarproject.briar.android.view.TextSendController;
import org.briarproject.briar.android.view.TextSendController.SendListener;
import org.briarproject.briar.android.view.UnreadMessageButton;
import org.briarproject.briar.api.client.NamedGroup;
import org.briarproject.briar.api.media.AttachmentHeader;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.annotation.CallSuper;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static org.briarproject.bramble.util.StringUtils.isNullOrEmpty;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public abstract class ThreadListActivity<G extends NamedGroup, I extends ThreadItem, A extends ThreadItemAdapter<I>>
		extends BriarActivity
		implements ThreadListListener<I>, SendListener, SharingListener,
		ThreadItemListener<I>, ThreadListDataSource {

	protected static final String KEY_REPLY_ID = "replyId";

	private static final Logger LOG =
			Logger.getLogger(ThreadListActivity.class.getName());

	protected A adapter;
	private ThreadScrollListener<I> scrollListener;
	protected BriarRecyclerView list;
	private LinearLayoutManager layoutManager;
	protected TextInputView textInput;
	protected TextSendController sendController;
	protected GroupId groupId;
	@Nullable
	private Parcelable layoutManagerState;
	@Nullable
	private MessageId replyId;

	protected abstract ThreadListController<G, I> getController();

	@Inject
	protected SharingController sharingController;

	@CallSuper
	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);

		setContentView(R.layout.activity_threaded_conversation);

		Intent i = getIntent();
		byte[] b = i.getByteArrayExtra(GROUP_ID);
		if (b == null) throw new IllegalStateException("No GroupId in intent.");
		groupId = new GroupId(b);
		getController().setGroupId(groupId);

		textInput = findViewById(R.id.text_input_container);
		sendController = new TextSendController(textInput, this, false);
		textInput.setSendController(sendController);
		textInput.setMaxTextLength(getMaxTextLength());
		textInput.setReady(true);

		UnreadMessageButton upButton = findViewById(R.id.upButton);
		UnreadMessageButton downButton = findViewById(R.id.downButton);

		list = findViewById(R.id.list);
		layoutManager = new LinearLayoutManager(this);
		list.setLayoutManager(layoutManager);
		adapter = createAdapter(layoutManager);
		list.setAdapter(adapter);
		scrollListener = new ThreadScrollListener<>(adapter, getController(),
				upButton, downButton);
		list.getRecyclerView().addOnScrollListener(scrollListener);

		upButton.setOnClickListener(v -> {
			int position = adapter.getVisibleUnreadPosTop();
			if (position != NO_POSITION) {
				list.getRecyclerView().scrollToPosition(position);
			}
		});
		downButton.setOnClickListener(v -> {
			int position = adapter.getVisibleUnreadPosBottom();
			if (position != NO_POSITION) {
				list.getRecyclerView().scrollToPosition(position);
			}
		});

		if (state != null) {
			byte[] replyIdBytes = state.getByteArray(KEY_REPLY_ID);
			if (replyIdBytes != null) replyId = new MessageId(replyIdBytes);
		}

		sharingController.setSharingListener(this);
		loadSharingContacts();
	}

	@Override
	@Nullable
	public MessageId getFirstVisibleMessageId() {
		if (layoutManager != null && adapter != null) {
			int position =
					layoutManager.findFirstVisibleItemPosition();
			I i = adapter.getItemAt(position);
			return i == null ? null : i.getId();
		}
		return null;
	}

	protected abstract A createAdapter(LinearLayoutManager layoutManager);

	protected void loadNamedGroup() {
		getController().loadNamedGroup(
				new UiResultExceptionHandler<G, DbException>(this) {
					@Override
					public void onResultUi(G groupItem) {
						onNamedGroupLoaded(groupItem);
					}

					@Override
					public void onExceptionUi(DbException exception) {
						handleException(exception);
					}
				});
	}

	@UiThread
	protected abstract void onNamedGroupLoaded(G groupItem);

	protected void loadItems() {
		int revision = adapter.getRevision();
		getController().loadItems(
				new UiResultExceptionHandler<ThreadItemList<I>, DbException>(
						this) {
					@Override
					public void onResultUi(ThreadItemList<I> items) {
						if (revision == adapter.getRevision()) {
							adapter.incrementRevision();
							if (items.isEmpty()) {
								list.showData();
							} else {
								displayItems(items);
								updateTextInput();
							}
						} else {
							LOG.info("Concurrent update, reloading");
							loadItems();
						}
					}

					@Override
					public void onExceptionUi(DbException exception) {
						handleException(exception);
					}
				});
	}

	private void displayItems(ThreadItemList<I> items) {
		adapter.setItems(items);
		MessageId messageId = items.getFirstVisibleItemId();
		if (messageId != null)
			adapter.setItemWithIdVisible(messageId);
		list.showData();
		if (layoutManagerState == null) {
			list.scrollToPosition(0);  // Scroll to the top
		} else {
			layoutManager.onRestoreInstanceState(layoutManagerState);
		}
	}

	protected void loadSharingContacts() {
		getController().loadSharingContacts(
				new UiResultExceptionHandler<Collection<ContactId>, DbException>(
						this) {
					@Override
					public void onResultUi(Collection<ContactId> contacts) {
						sharingController.addAll(contacts);
						int online = sharingController.getOnlineCount();
						setToolbarSubTitle(contacts.size(), online);
					}

					@Override
					public void onExceptionUi(DbException exception) {
						handleException(exception);
					}
				});
	}

	@CallSuper
	@Override
	public void onStart() {
		super.onStart();
		sharingController.onStart();
		loadItems();
		list.startPeriodicUpdate();
	}

	@CallSuper
	@Override
	public void onStop() {
		super.onStop();
		sharingController.onStop();
		list.stopPeriodicUpdate();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (layoutManager != null) {
			layoutManagerState = layoutManager.onSaveInstanceState();
			outState.putParcelable("layoutManager", layoutManagerState);
		}
		if (replyId != null) {
			outState.putByteArray(KEY_REPLY_ID, replyId.getBytes());
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		layoutManagerState = savedInstanceState.getParcelable("layoutManager");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				supportFinishAfterTransition();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		if (adapter.getHighlightedItem() != null) {
			textInput.clearText();
			replyId = null;
			updateTextInput();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void onReplyClick(I item) {
		replyId = item.getId();
		updateTextInput();
		// FIXME This does not work for a hardware keyboard
		if (textInput.isKeyboardOpen()) {
			scrollToItemAtTop(item);
		} else {
			// wait with scrolling until keyboard opened
			textInput.setOnKeyboardShownListener(() -> {
				scrollToItemAtTop(item);
				textInput.setOnKeyboardShownListener(null);
			});
		}
	}

	@Override
	public void onSharingInfoUpdated(int total, int online) {
		setToolbarSubTitle(total, online);
	}

	@Override
	public void onInvitationAccepted(ContactId c) {
		sharingController.add(c);
		setToolbarSubTitle(sharingController.getTotalCount(),
				sharingController.getOnlineCount());
	}

	protected void setToolbarSubTitle(int total, int online) {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setSubtitle(
					getString(R.string.shared_with, total, online));
		}
	}

	private void scrollToItemAtTop(I item) {
		int position = adapter.findItemPosition(item);
		if (position != NO_POSITION) {
			layoutManager
					.scrollToPositionWithOffset(position, 0);
		}
	}

	protected void displaySnackbar(@StringRes int stringId) {
		new BriarSnackbarBuilder()
				.make(list, stringId, Snackbar.LENGTH_SHORT)
				.show();
	}

	private void updateTextInput() {
		if (replyId != null) {
			textInput.setHint(R.string.forum_message_reply_hint);
			textInput.showSoftKeyboard();
		} else {
			textInput.setHint(R.string.forum_new_message_hint);
		}
		adapter.setHighlightedItem(replyId);
	}

	@Override
	public void onSendClick(@Nullable String text,
			List<AttachmentHeader> headers) {
		if (isNullOrEmpty(text)) throw new AssertionError();

		I replyItem = adapter.getHighlightedItem();
		UiResultExceptionHandler<I, DbException> handler =
				new UiResultExceptionHandler<I, DbException>(this) {
					@Override
					public void onResultUi(I result) {
						addItem(result, true);
					}

					@Override
					public void onExceptionUi(DbException exception) {
						handleException(exception);
					}
				};
		getController().createAndStoreMessage(text, replyItem, handler);
		textInput.hideSoftKeyboard();
		textInput.clearText();
		replyId = null;
		updateTextInput();
	}

	protected abstract int getMaxTextLength();

	@Override
	public void onItemReceived(I item) {
		addItem(item, false);
	}

	@Override
	public void onGroupRemoved() {
		supportFinishAfterTransition();
	}

	private void addItem(I item, boolean isLocal) {
		adapter.incrementRevision();
		MessageId parent = item.getParentId();
		if (parent != null && !adapter.contains(parent)) {
			// We've incremented the adapter's revision, so the item will be
			// loaded when its parent has been loaded
			LOG.info("Ignoring item with missing parent");
			return;
		}
		adapter.add(item);

		if (isLocal) {
			scrollToItemAtTop(item);
		} else {
			scrollListener.updateUnreadButtons(layoutManager);
		}
	}

}

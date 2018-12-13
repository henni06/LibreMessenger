package org.briarproject.briar.android.conversation;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.transition.Transition;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.briarproject.bramble.api.Pair;
import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.contact.ContactManager;
import org.briarproject.bramble.api.contact.event.ContactRemovedEvent;
import org.briarproject.bramble.api.crypto.CryptoExecutor;
import org.briarproject.bramble.api.db.DatabaseExecutor;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.NoSuchContactException;
import org.briarproject.bramble.api.event.Event;
import org.briarproject.bramble.api.event.EventBus;
import org.briarproject.bramble.api.event.EventListener;
import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.bramble.api.plugin.ConnectionRegistry;
import org.briarproject.bramble.api.plugin.event.ContactConnectedEvent;
import org.briarproject.bramble.api.plugin.event.ContactDisconnectedEvent;
import org.briarproject.bramble.api.settings.Settings;
import org.briarproject.bramble.api.settings.SettingsManager;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.bramble.api.sync.event.MessagesAckedEvent;
import org.briarproject.bramble.api.sync.event.MessagesSentEvent;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.activity.BriarActivity;
import org.briarproject.briar.android.blog.BlogActivity;
import org.briarproject.briar.android.conversation.ConversationVisitor.AttachmentCache;
import org.briarproject.briar.android.conversation.ConversationVisitor.TextCache;
import org.briarproject.briar.android.forum.ForumActivity;
import org.briarproject.briar.android.introduction.IntroductionActivity;
import org.briarproject.briar.android.privategroup.conversation.GroupActivity;
import org.briarproject.briar.android.view.BriarRecyclerView;
import org.briarproject.briar.android.view.ImagePreview;
import org.briarproject.briar.android.view.TextAttachmentController;
import org.briarproject.briar.android.view.TextAttachmentController.AttachImageListener;
import org.briarproject.briar.android.view.TextInputView;
import org.briarproject.briar.android.view.TextSendController;
import org.briarproject.briar.android.view.TextSendController.SendListener;
import org.briarproject.briar.api.android.AndroidNotificationManager;
import org.briarproject.briar.api.blog.BlogSharingManager;
import org.briarproject.briar.api.client.ProtocolStateException;
import org.briarproject.briar.api.client.SessionId;
import org.briarproject.briar.api.conversation.ConversationManager;
import org.briarproject.briar.api.conversation.ConversationMessageHeader;
import org.briarproject.briar.api.conversation.ConversationRequest;
import org.briarproject.briar.api.conversation.ConversationResponse;
import org.briarproject.briar.api.conversation.event.ConversationMessageReceivedEvent;
import org.briarproject.briar.api.forum.ForumSharingManager;
import org.briarproject.briar.api.introduction.IntroductionManager;
import org.briarproject.briar.api.messaging.Attachment;
import org.briarproject.briar.api.messaging.AttachmentHeader;
import org.briarproject.briar.api.messaging.MessagingManager;
import org.briarproject.briar.api.messaging.PrivateMessageFactory;
import org.briarproject.briar.api.messaging.PrivateMessageHeader;
import org.briarproject.briar.api.privategroup.invitation.GroupInvitationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import de.hdodenhof.circleimageview.CircleImageView;
import im.delight.android.identicons.IdenticonDrawable;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt.PromptStateChangeListener;

import static android.os.Build.VERSION.SDK_INT;
import static android.support.v4.app.ActivityOptionsCompat.makeSceneTransitionAnimation;
import static android.support.v4.view.ViewCompat.setTransitionName;
import static android.support.v7.util.SortedList.INVALID_POSITION;
import static android.view.Gravity.RIGHT;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static org.briarproject.bramble.util.LogUtils.logDuration;
import static org.briarproject.bramble.util.LogUtils.logException;
import static org.briarproject.bramble.util.LogUtils.now;
import static org.briarproject.bramble.util.StringUtils.isNullOrEmpty;
import static org.briarproject.briar.android.TestingConstants.FEATURE_FLAG_IMAGE_ATTACHMENTS;
import static org.briarproject.briar.android.activity.RequestCodes.REQUEST_ATTACH_IMAGE;
import static org.briarproject.briar.android.activity.RequestCodes.REQUEST_INTRODUCTION;
import static org.briarproject.briar.android.conversation.ImageActivity.ATTACHMENT_POSITION;
import static org.briarproject.briar.android.conversation.ImageActivity.DATE;
import static org.briarproject.briar.android.conversation.ImageActivity.ATTACHMENTS;
import static org.briarproject.briar.android.conversation.ImageActivity.NAME;
import static org.briarproject.briar.android.settings.SettingsFragment.SETTINGS_NAMESPACE;
import static org.briarproject.briar.android.util.UiUtils.getAvatarTransitionName;
import static org.briarproject.briar.android.util.UiUtils.getBulbTransitionName;
import static org.briarproject.briar.android.util.UiUtils.observeOnce;
import static org.briarproject.briar.api.messaging.MessagingConstants.MAX_PRIVATE_MESSAGE_TEXT_LENGTH;
import static uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt.STATE_DISMISSED;
import static uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt.STATE_FINISHED;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class ConversationActivity extends BriarActivity
		implements EventListener, ConversationListener, SendListener,
		TextCache, AttachmentCache, AttachImageListener {

	public static final String CONTACT_ID = "briar.CONTACT_ID";

	private static final Logger LOG =
			Logger.getLogger(ConversationActivity.class.getName());
	private static final String SHOW_ONBOARDING_INTRODUCTION =
			"showOnboardingIntroduction";

	@Inject
	AndroidNotificationManager notificationManager;
	@Inject
	ConnectionRegistry connectionRegistry;
	@Inject
	@CryptoExecutor
	Executor cryptoExecutor;

	private final Map<MessageId, String> textCache = new ConcurrentHashMap<>();
	private AttachmentController attachmentController;

	private ConversationViewModel viewModel;
	private ConversationVisitor visitor;
	private ConversationAdapter adapter;
	private Toolbar toolbar;
	private CircleImageView toolbarAvatar;
	private ImageView toolbarStatus;
	private TextView toolbarTitle;
	private BriarRecyclerView list;
	private LinearLayoutManager layoutManager;
	private TextInputView textInputView;
	private TextSendController sendController;

	// Fields that are accessed from background threads must be volatile
	@Inject
	volatile ContactManager contactManager;
	@Inject
	volatile MessagingManager messagingManager;
	@Inject
	volatile ConversationManager conversationManager;
	@Inject
	volatile EventBus eventBus;
	@Inject
	volatile SettingsManager settingsManager;
	@Inject
	volatile PrivateMessageFactory privateMessageFactory;
	@Inject
	volatile IntroductionManager introductionManager;
	@Inject
	volatile ForumSharingManager forumSharingManager;
	@Inject
	volatile BlogSharingManager blogSharingManager;
	@Inject
	volatile GroupInvitationManager groupInvitationManager;
	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private volatile ContactId contactId;

	private final Observer<String> contactNameObserver = name -> {
		requireNonNull(name);
		loadMessages();
	};

	@Override
	public void onCreate(@Nullable Bundle state) {
		if (SDK_INT >= 21) {
			// Spurious lint warning - using END causes a crash
			@SuppressLint("RtlHardcoded")
			Transition slide = new Slide(RIGHT);
			setSceneTransitionAnimation(slide, null, slide);
		}
		super.onCreate(state);

		Intent i = getIntent();
		int id = i.getIntExtra(CONTACT_ID, -1);
		if (id == -1) throw new IllegalStateException();
		contactId = new ContactId(id);

		viewModel = ViewModelProviders.of(this, viewModelFactory)
				.get(ConversationViewModel.class);
		viewModel.setContactId(contactId);
		attachmentController = viewModel.getAttachmentController();

		setContentView(R.layout.activity_conversation);

		// Custom Toolbar
		toolbar = requireNonNull(setUpCustomToolbar(true));
		toolbarAvatar = toolbar.findViewById(R.id.contactAvatar);
		toolbarStatus = toolbar.findViewById(R.id.contactStatus);
		toolbarTitle = toolbar.findViewById(R.id.contactName);

		observeOnce(viewModel.getContactAuthorId(), this, authorId -> {
			requireNonNull(authorId);
			toolbarAvatar.setImageDrawable(
					new IdenticonDrawable(authorId.getBytes()));
		});
		viewModel.getContactDisplayName().observe(this, contactName -> {
			requireNonNull(contactName);
			toolbarTitle.setText(contactName);
		});
		viewModel.isContactDeleted().observe(this, deleted -> {
			requireNonNull(deleted);
			if (deleted) finish();
		});
		viewModel.getAddedPrivateMessage()
				.observe(this, this::onAddedPrivateMessage);

		setTransitionName(toolbarAvatar, getAvatarTransitionName(contactId));
		setTransitionName(toolbarStatus, getBulbTransitionName(contactId));

		visitor = new ConversationVisitor(this, this, this,
				viewModel.getContactDisplayName());
		adapter = new ConversationAdapter(this, this);
		list = findViewById(R.id.conversationView);
		layoutManager = new LinearLayoutManager(this);
		list.setLayoutManager(layoutManager);
		list.setAdapter(adapter);
		list.setEmptyText(getString(R.string.no_private_messages));

		textInputView = findViewById(R.id.text_input_container);
		if (FEATURE_FLAG_IMAGE_ATTACHMENTS) {
			ImagePreview imagePreview = findViewById(R.id.imagePreview);
			sendController = new TextAttachmentController(textInputView,
					imagePreview, this, this);
		} else {
			sendController = new TextSendController(textInputView, this, false);
		}
		textInputView.setSendController(sendController);
		textInputView.setMaxTextLength(MAX_PRIVATE_MESSAGE_TEXT_LENGTH);
		textInputView.setEnabled(false);
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	protected void onActivityResult(int request, int result, Intent data) {
		super.onActivityResult(request, result, data);

		if (request == REQUEST_INTRODUCTION && result == RESULT_OK) {
			Snackbar snackbar = Snackbar.make(list, R.string.introduction_sent,
					Snackbar.LENGTH_SHORT);
			snackbar.getView().setBackgroundResource(R.color.briar_primary);
			snackbar.show();
		} else if (request == REQUEST_ATTACH_IMAGE && result == RESULT_OK) {
			// remove cast when removing FEATURE_FLAG_IMAGE_ATTACHMENTS
			((TextAttachmentController) sendController).onImageReceived(data);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		eventBus.addListener(this);
		notificationManager.blockContactNotification(contactId);
		notificationManager.clearContactNotification(contactId);
		displayContactOnlineStatus();
		viewModel.getContactDisplayName().observe(this, contactNameObserver);
		list.startPeriodicUpdate();
	}

	@Override
	public void onStop() {
		super.onStop();
		eventBus.removeListener(this);
		notificationManager.unblockContactNotification(contactId);
		viewModel.getContactDisplayName().removeObserver(contactNameObserver);
		list.stopPeriodicUpdate();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.conversation_actions, menu);

		enableIntroductionActionIfAvailable(
				menu.findItem(R.id.action_introduction));
		enableAliasActionIfAvailable(
				menu.findItem(R.id.action_set_alias));

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
			case R.id.action_introduction:
				if (contactId == null) return false;
				Intent intent = new Intent(this, IntroductionActivity.class);
				intent.putExtra(CONTACT_ID, contactId.getInt());
				startActivityForResult(intent, REQUEST_INTRODUCTION);
				return true;
			case R.id.action_set_alias:
				AliasDialogFragment.newInstance().show(
						getSupportFragmentManager(), AliasDialogFragment.TAG);
				return true;
			case R.id.action_social_remove_person:
				askToRemoveContact();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void displayContactOnlineStatus() {
		runOnUiThreadUnlessDestroyed(() -> {
			if (connectionRegistry.isConnected(contactId)) {
				toolbarStatus.setImageDrawable(ContextCompat
						.getDrawable(ConversationActivity.this,
								R.drawable.contact_online));
				toolbarStatus
						.setContentDescription(getString(R.string.online));
			} else {
				toolbarStatus.setImageDrawable(ContextCompat
						.getDrawable(ConversationActivity.this,
								R.drawable.contact_offline));
				toolbarStatus
						.setContentDescription(getString(R.string.offline));
			}
		});
	}

	private void loadMessages() {
		int revision = adapter.getRevision();
		runOnDbThread(() -> {
			try {
				long start = now();
				Collection<ConversationMessageHeader> headers =
						conversationManager.getMessageHeaders(contactId);
				logDuration(LOG, "Loading messages", start);
				// Sort headers by timestamp in *descending* order
				List<ConversationMessageHeader> sorted =
						new ArrayList<>(headers);
				sort(sorted, (a, b) ->
						Long.compare(b.getTimestamp(), a.getTimestamp()));
				if (!sorted.isEmpty()) {
					// If the latest header is a private message, eagerly load
					// its text so we can set the scroll position correctly
					ConversationMessageHeader latest = sorted.get(0);
					if (latest instanceof PrivateMessageHeader) {
						MessageId id = latest.getId();
						PrivateMessageHeader h = (PrivateMessageHeader) latest;
						if (h.hasText()) {
							String text = textCache.get(id);
							if (text == null) {
								LOG.info(
										"Eagerly loading text of latest message");
								text = messagingManager.getMessageText(id);
								textCache.put(id, text);
							}
						}
						if (h.getAttachmentHeaders().size() == 1) {
							List<AttachmentItem> items =
									attachmentController.get(id);
							if (items == null) {
								LOG.info(
										"Eagerly loading image size for latest message");
								items = attachmentController.getAttachmentItems(
										attachmentController
												.getMessageAttachments(
														h.getAttachmentHeaders()));
								attachmentController.put(id, items);
							}
						}
					}
				}
				displayMessages(revision, sorted);
			} catch (NoSuchContactException e) {
				finishOnUiThread();
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	private void displayMessages(int revision,
			Collection<ConversationMessageHeader> headers) {
		runOnUiThreadUnlessDestroyed(() -> {
			if (revision == adapter.getRevision()) {
				adapter.incrementRevision();
				textInputView.setEnabled(true);
				List<ConversationItem> items = createItems(headers);
				adapter.addAll(items);
				list.showData();
				// Scroll to the bottom
				list.scrollToPosition(adapter.getItemCount() - 1);
			} else {
				LOG.info("Concurrent update, reloading");
				loadMessages();
			}
		});
	}

	/**
	 * Creates ConversationItems from headers loaded from the database.
	 * <p>
	 * Attention: Call this only after contactName has been initialized.
	 */
	@SuppressWarnings("ConstantConditions")
	private List<ConversationItem> createItems(
			Collection<ConversationMessageHeader> headers) {
		List<ConversationItem> items = new ArrayList<>(headers.size());
		for (ConversationMessageHeader h : headers)
			items.add(h.accept(visitor));
		return items;
	}

	private void loadMessageText(MessageId m) {
		runOnDbThread(() -> {
			try {
				long start = now();
				String text = messagingManager.getMessageText(m);
				logDuration(LOG, "Loading text", start);
				displayMessageText(m, text);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	private void displayMessageText(MessageId m, String text) {
		runOnUiThreadUnlessDestroyed(() -> {
			textCache.put(m, text);
			Pair<Integer, ConversationMessageItem> pair =
					adapter.getMessageItem(m);
			if (pair != null) {
				pair.getSecond().setText(text);
				boolean bottom = adapter.isScrolledToBottom(layoutManager);
				adapter.notifyItemChanged(pair.getFirst());
				if (bottom) list.scrollToPosition(adapter.getItemCount() - 1);
			}
		});
	}

	private void loadMessageAttachments(MessageId messageId,
			List<AttachmentHeader> headers) {
		runOnDbThread(() -> {
			try {
				List<Pair<AttachmentHeader, Attachment>> attachments =
						attachmentController.getMessageAttachments(headers);
				// TODO move getting the items off to IoExecutor, if size == 1
				List<AttachmentItem> items =
						attachmentController.getAttachmentItems(attachments);
				displayMessageAttachments(messageId, items);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	private void displayMessageAttachments(MessageId m,
			List<AttachmentItem> items) {
		runOnUiThreadUnlessDestroyed(() -> {
			attachmentController.put(m, items);
			Pair<Integer, ConversationMessageItem> pair =
					adapter.getMessageItem(m);
			if (pair != null) {
				pair.getSecond().setAttachments(items);
				boolean bottom = adapter.isScrolledToBottom(layoutManager);
				adapter.notifyItemChanged(pair.getFirst());
				if (bottom) list.scrollToPosition(adapter.getItemCount() - 1);
			}
		});
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof ContactRemovedEvent) {
			ContactRemovedEvent c = (ContactRemovedEvent) e;
			if (c.getContactId().equals(contactId)) {
				LOG.info("Contact removed");
				finishOnUiThread();
			}
		} else if (e instanceof ConversationMessageReceivedEvent) {
			ConversationMessageReceivedEvent p =
					(ConversationMessageReceivedEvent) e;
			if (p.getContactId().equals(contactId)) {
				LOG.info("Message received, adding");
				onNewConversationMessage(p.getMessageHeader());
			}
		} else if (e instanceof MessagesSentEvent) {
			MessagesSentEvent m = (MessagesSentEvent) e;
			if (m.getContactId().equals(contactId)) {
				LOG.info("Messages sent");
				markMessages(m.getMessageIds(), true, false);
			}
		} else if (e instanceof MessagesAckedEvent) {
			MessagesAckedEvent m = (MessagesAckedEvent) e;
			if (m.getContactId().equals(contactId)) {
				LOG.info("Messages acked");
				markMessages(m.getMessageIds(), true, true);
			}
		} else if (e instanceof ContactConnectedEvent) {
			ContactConnectedEvent c = (ContactConnectedEvent) e;
			if (c.getContactId().equals(contactId)) {
				LOG.info("Contact connected");
				displayContactOnlineStatus();
			}
		} else if (e instanceof ContactDisconnectedEvent) {
			ContactDisconnectedEvent c = (ContactDisconnectedEvent) e;
			if (c.getContactId().equals(contactId)) {
				LOG.info("Contact disconnected");
				displayContactOnlineStatus();
			}
		}
	}

	private void addConversationItem(ConversationItem item) {
		runOnUiThreadUnlessDestroyed(() -> {
			boolean bottom = adapter.isScrolledToBottom(layoutManager);
			adapter.incrementRevision();
			adapter.add(item);
			if (bottom) list.scrollToPosition(adapter.getItemCount() - 1);
		});
	}

	private void onNewConversationMessage(ConversationMessageHeader h) {
		runOnUiThreadUnlessDestroyed(() -> {
			if (h instanceof ConversationRequest ||
					h instanceof ConversationResponse) {
				// contact name might not have been loaded
				observeOnce(viewModel.getContactDisplayName(), this,
						name -> addConversationItem(h.accept(visitor)));
			} else {
				// visitor also loads message text (if existing)
				addConversationItem(h.accept(visitor));
			}
		});
	}

	private void markMessages(Collection<MessageId> messageIds, boolean sent,
			boolean seen) {
		runOnUiThreadUnlessDestroyed(() -> {
			adapter.incrementRevision();
			Set<MessageId> messages = new HashSet<>(messageIds);
			SparseArray<ConversationItem> list = adapter.getOutgoingMessages();
			for (int i = 0; i < list.size(); i++) {
				ConversationItem item = list.valueAt(i);
				if (messages.contains(item.getId())) {
					item.setSent(sent);
					item.setSeen(seen);
					adapter.notifyItemChanged(list.keyAt(i));
				}
			}
		});
	}

	@Override
	public void onAttachImage(Intent intent) {
		startActivityForResult(intent, REQUEST_ATTACH_IMAGE);
	}

	@Override
	public void onSendClick(@Nullable String text, List<Uri> imageUris) {
		if (isNullOrEmpty(text) && imageUris.isEmpty())
			throw new AssertionError();
		long timestamp = System.currentTimeMillis();
		timestamp = Math.max(timestamp, getMinTimestampForNewMessage());
		viewModel.sendMessage(text, imageUris, timestamp);
		textInputView.clearText();
	}

	private long getMinTimestampForNewMessage() {
		// Don't use an earlier timestamp than the newest message
		ConversationItem item = adapter.getLastItem();
		return item == null ? 0 : item.getTime() + 1;
	}

	private void onAddedPrivateMessage(@Nullable PrivateMessageHeader h) {
		if (h == null) return;
		addConversationItem(h.accept(visitor));
		viewModel.onAddedPrivateMessageSeen();
	}

	private void askToRemoveContact() {
		DialogInterface.OnClickListener okListener =
				(dialog, which) -> removeContact();
		AlertDialog.Builder builder =
				new AlertDialog.Builder(ConversationActivity.this,
						R.style.BriarDialogTheme);
		builder.setTitle(getString(R.string.dialog_title_delete_contact));
		builder.setMessage(getString(R.string.dialog_message_delete_contact));
		builder.setNegativeButton(R.string.delete, okListener);
		builder.setPositiveButton(R.string.cancel, null);
		builder.show();
	}

	private void removeContact() {
		runOnDbThread(() -> {
			try {
				contactManager.removeContact(contactId);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			} finally {
				finishAfterContactRemoved();
			}
		});
	}

	private void finishAfterContactRemoved() {
		runOnUiThreadUnlessDestroyed(() -> {
			String deleted = getString(R.string.contact_deleted_toast);
			Toast.makeText(ConversationActivity.this, deleted, LENGTH_SHORT)
					.show();
			supportFinishAfterTransition();
		});
	}

	private void enableIntroductionActionIfAvailable(MenuItem item) {
		runOnDbThread(() -> {
			try {
				if (contactManager.getActiveContacts().size() > 1) {
					enableIntroductionAction(item);
					Settings settings =
							settingsManager.getSettings(SETTINGS_NAMESPACE);
					if (settings.getBoolean(SHOW_ONBOARDING_INTRODUCTION,
							true)) {
						showIntroductionOnboarding();
					}
				}
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	private void enableAliasActionIfAvailable(MenuItem item) {
		observeOnce(viewModel.getContact(), this, c -> item.setEnabled(true));
	}

	private void enableIntroductionAction(MenuItem item) {
		runOnUiThreadUnlessDestroyed(() -> item.setEnabled(true));
	}

	private void showIntroductionOnboarding() {
		runOnUiThreadUnlessDestroyed(() -> {
			// find view of overflow icon
			View target = null;
			for (int i = 0; i < toolbar.getChildCount(); i++) {
				if (toolbar.getChildAt(i) instanceof ActionMenuView) {
					ActionMenuView menu =
							(ActionMenuView) toolbar.getChildAt(i);
					target = menu.getChildAt(menu.getChildCount() - 1);
					break;
				}
			}
			if (target == null) {
				LOG.warning("No Overflow Icon found!");
				return;
			}

			PromptStateChangeListener listener = (prompt, state) -> {
				if (state == STATE_DISMISSED || state == STATE_FINISHED) {
					introductionOnboardingSeen();
				}
			};
			new MaterialTapTargetPrompt.Builder(ConversationActivity.this,
					R.style.OnboardingDialogTheme).setTarget(target)
					.setPrimaryText(R.string.introduction_onboarding_title)
					.setSecondaryText(R.string.introduction_onboarding_text)
					.setIcon(R.drawable.ic_more_vert_accent)
					.setPromptStateChangeListener(listener)
					.show();
		});
	}

	private void introductionOnboardingSeen() {
		runOnDbThread(() -> {
			try {
				Settings settings = new Settings();
				settings.putBoolean(SHOW_ONBOARDING_INTRODUCTION, false);
				settingsManager.mergeSettings(settings, SETTINGS_NAMESPACE);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	@Override
	public void onItemVisible(ConversationItem item) {
		if (!item.isRead()) markMessageRead(item.getGroupId(), item.getId());
	}

	private void markMessageRead(GroupId g, MessageId m) {
		runOnDbThread(() -> {
			try {
				long start = now();
				messagingManager.setReadFlag(g, m, true);
				logDuration(LOG, "Marking read", start);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	@UiThread
	@Override
	public void respondToRequest(ConversationRequestItem item, boolean accept) {
		item.setAnswered();
		int position = adapter.findItemPosition(item);
		if (position != INVALID_POSITION) {
			adapter.notifyItemChanged(position, item);
		}
		runOnDbThread(() -> {
			long timestamp = System.currentTimeMillis();
			timestamp = Math.max(timestamp, getMinTimestampForNewMessage());
			try {
				switch (item.getRequestType()) {
					case INTRODUCTION:
						respondToIntroductionRequest(item.getSessionId(),
								accept, timestamp);
						break;
					case FORUM:
						respondToForumRequest(item.getSessionId(), accept);
						break;
					case BLOG:
						respondToBlogRequest(item.getSessionId(), accept);
						break;
					case GROUP:
						respondToGroupRequest(item.getSessionId(), accept);
						break;
					default:
						throw new IllegalArgumentException(
								"Unknown Request Type");
				}
				loadMessages();
			} catch (ProtocolStateException e) {
				// Action is no longer valid - reloading should solve the issue
				logException(LOG, INFO, e);
			} catch (DbException e) {
				// TODO show an error message
				logException(LOG, WARNING, e);
			}
		});
	}

	@UiThread
	@Override
	public void openRequestedShareable(ConversationRequestItem item) {
		if (item.getRequestedGroupId() == null)
			throw new IllegalArgumentException();
		Intent i;
		switch (item.getRequestType()) {
			case FORUM:
				i = new Intent(this, ForumActivity.class);
				break;
			case BLOG:
				i = new Intent(this, BlogActivity.class);
				break;
			case GROUP:
				i = new Intent(this, GroupActivity.class);
				break;
			default:
				throw new IllegalArgumentException("Unknown Request Type");
		}
		i.putExtra(GROUP_ID, item.getRequestedGroupId().getBytes());
		startActivity(i);
	}

	@Override
	public void onAttachmentClicked(View view,
			ConversationMessageItem messageItem, AttachmentItem item) {
		String name;
		if (messageItem.isIncoming()) {
			// must be available when items are being displayed
			name = viewModel.getContactDisplayName().getValue();
		} else {
			name = getString(R.string.you);
		}
		ArrayList<AttachmentItem> attachments =
				new ArrayList<>(messageItem.getAttachments());
		Intent i = new Intent(this, ImageActivity.class);
		i.putParcelableArrayListExtra(ATTACHMENTS, attachments);
		i.putExtra(ATTACHMENT_POSITION, attachments.indexOf(item));
		i.putExtra(NAME, name);
		i.putExtra(DATE, messageItem.getTime());
		if (SDK_INT >= 23) {
			String transitionName = item.getTransitionName();
			ActivityOptionsCompat options =
					makeSceneTransitionAnimation(this, view, transitionName);
			ActivityCompat.startActivity(this, i, options.toBundle());
		} else {
			// work-around for android bug #224270
			startActivity(i);
		}
	}

	@DatabaseExecutor
	private void respondToIntroductionRequest(SessionId sessionId,
			boolean accept, long time) throws DbException {
		introductionManager.respondToIntroduction(contactId, sessionId, time,
				accept);
	}

	@DatabaseExecutor
	private void respondToForumRequest(SessionId id, boolean accept)
			throws DbException {
		forumSharingManager.respondToInvitation(contactId, id, accept);
	}

	@DatabaseExecutor
	private void respondToBlogRequest(SessionId id, boolean accept)
			throws DbException {
		blogSharingManager.respondToInvitation(contactId, id, accept);
	}

	@DatabaseExecutor
	private void respondToGroupRequest(SessionId id, boolean accept)
			throws DbException {
		groupInvitationManager.respondToInvitation(contactId, id, accept);
	}

	@Nullable
	@Override
	public String getText(MessageId m) {
		String text = textCache.get(m);
		if (text == null) loadMessageText(m);
		return text;
	}

	@Override
	public List<AttachmentItem> getAttachmentItems(MessageId m,
			List<AttachmentHeader> headers) {
		List<AttachmentItem> attachments = attachmentController.get(m);
		if (attachments == null) {
			loadMessageAttachments(m, headers);
			return emptyList();
		}
		return attachments;
	}

}

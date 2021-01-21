package org.briarproject.briar.android.view;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.attachment.AttachmentItemResult;
import org.briarproject.briar.android.attachment.AttachmentManager;
import org.briarproject.briar.android.attachment.AttachmentResult;
import org.briarproject.briar.android.util.UiUtils;
import org.briarproject.briar.android.view.ImagePreview.ImagePreviewListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.customview.view.AbsSavedState;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

import static android.os.Build.VERSION.SDK_INT;
import static android.view.View.GONE;
import static android.widget.Toast.LENGTH_LONG;
import static androidx.core.content.ContextCompat.getColor;
import static androidx.customview.view.AbsSavedState.EMPTY_STATE;
import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static org.briarproject.briar.android.util.UiUtils.resolveColorAttribute;
import static org.briarproject.briar.api.messaging.MessagingConstants.MAX_ATTACHMENTS_PER_MESSAGE;

@UiThread
@NotNullByDefault
public class TextAttachmentController extends TextSendController
		implements ImagePreviewListener {

	private final ImagePreview imagePreview;
	private final AttachmentListener attachmentListener;
	private final CompositeSendButton sendButton;
	private final AttachmentManager attachmentManager;

	private final List<Uri> imageUris = new ArrayList<>();
	private final CharSequence textHint;
	private boolean loadingUris = false;

	public TextAttachmentController(TextInputView v, ImagePreview imagePreview,
			AttachmentListener attachmentListener,
			AttachmentManager attachmentManager) {
		super(v, attachmentListener, false);
		this.attachmentListener = attachmentListener;
		this.imagePreview = imagePreview;
		this.attachmentManager = attachmentManager;
		this.imagePreview.setImagePreviewListener(this);

		sendButton = (CompositeSendButton) compositeSendButton;
		sendButton.setOnImageClickListener(view -> onImageButtonClicked());

		textHint = textInput.getHint();
	}

	@Override
	protected void updateViewState() {
		textInput.setEnabled(ready && !loadingUris);
		boolean sendEnabled = ready && !loadingUris &&
				(!textIsEmpty || canSendEmptyText());
		if (loadingUris) {
			sendButton.showProgress(true);
		} else if (imageUris.isEmpty()) {
			sendButton.showProgress(false);
			sendButton.showImageButton(textIsEmpty, sendEnabled);
		} else {
			sendButton.showProgress(false);
			sendButton.showImageButton(false, sendEnabled);
		}
	}

	@Override
	public void onSendEvent() {
		if (canSend()) {
			if (loadingUris) throw new AssertionError();
			listener.onSendClick(textInput.getText(),
					attachmentManager.getAttachmentHeadersForSending());
			reset();
		}
	}

	@Override
	protected boolean canSendEmptyText() {
		return !imageUris.isEmpty();
	}

	public void setImagesSupported() {
		sendButton.setImagesSupported();
	}

	private void onImageButtonClicked() {
		if (!sendButton.hasImageSupport()) {
			Context ctx = imagePreview.getContext();
			Builder builder = new Builder(ctx, R.style.OnboardingDialogTheme);
			builder.setTitle(
					ctx.getString(R.string.dialog_title_no_image_support));
			builder.setMessage(
					ctx.getString(R.string.dialog_message_no_image_support));
			builder.setPositiveButton(R.string.got_it, null);
			builder.show();
			return;
		}
		Intent intent = UiUtils.createSelectImageIntent(true);
		if (attachmentListener.getLifecycle().getCurrentState() != DESTROYED) {
			attachmentListener.onAttachImage(intent);
		}
	}

	/**
	 * This is called with the result Intent returned by the Activity started
	 * with {@link UiUtils#createSelectImageIntent(boolean)}.
	 * <p>
	 * This method must be called at most once per call to
	 * {@link AttachmentListener#onAttachImage(Intent)}.
	 * Normally, this is true if called from
	 * {@link Activity#onActivityResult(int, int, Intent)} since this is called
	 * at most once per call to
	 * {@link Activity#startActivityForResult(Intent, int)}.
	 */
	@SuppressWarnings("JavadocReference")
	public void onImageReceived(@Nullable Intent resultData) {
		if (resultData == null) return;
		if (loadingUris || !imageUris.isEmpty()) throw new AssertionError();
		List<Uri> newUris = new ArrayList<>();
		if (resultData.getData() != null) {
			newUris.add(resultData.getData());
			onNewUris(false, newUris);
		} else if (SDK_INT >= 18 && resultData.getClipData() != null) {
			ClipData clipData = resultData.getClipData();
			for (int i = 0; i < clipData.getItemCount(); i++) {
				newUris.add(clipData.getItemAt(i).getUri());
			}
			onNewUris(false, newUris);
		}
	}

	private void onNewUris(boolean restart, List<Uri> newUris) {
		if (newUris.isEmpty()) return;
		if (loadingUris) throw new AssertionError();
		loadingUris = true;
		if (newUris.size() > MAX_ATTACHMENTS_PER_MESSAGE) {
			newUris = newUris.subList(0, MAX_ATTACHMENTS_PER_MESSAGE);
			attachmentListener.onTooManyAttachments();
		}
		imageUris.addAll(newUris);
		updateViewState();
		textInput.setHint(R.string.image_caption_hint);
		List<ImagePreviewItem> items = ImagePreviewItem.fromUris(imageUris);
		imagePreview.showPreview(items);
		// store attachments and show preview when successful
		LiveData<AttachmentResult> result =
				attachmentManager.storeAttachments(imageUris, restart);
		result.observe(attachmentListener, new Observer<AttachmentResult>() {
			@Override
			public void onChanged(@Nullable AttachmentResult attachmentResult) {
				if (attachmentResult == null) {
					// The fresh LiveData was deliberately set to null.
					// This means that we can stop observing it.
					result.removeObserver(this);
				} else {
					boolean noError = onNewAttachmentItemResults(
							attachmentResult.getItemResults());
					if (noError && attachmentResult.isFinished()) {
						onAllAttachmentsCreated();
						result.removeObserver(this);
					}
				}
			}
		});
	}

	private boolean onNewAttachmentItemResults(
			Collection<AttachmentItemResult> itemResults) {
		if (!loadingUris) throw new AssertionError();
		for (AttachmentItemResult result : itemResults) {
			if (result.hasError()) {
				onError(result.getErrorMsg());
				return false;
			} else {
				imagePreview.loadPreviewImage(result);
			}
		}
		return true;
	}

	private void onAllAttachmentsCreated() {
		if (!loadingUris) throw new AssertionError();
		loadingUris = false;
		updateViewState();
	}

	private void reset() {
		// restore hint
		textInput.setHint(textHint);
		// hide image layout
		imagePreview.setVisibility(GONE);
		// reset image URIs
		imageUris.clear();
		// definitely not loading anymore
		loadingUris = false;
		// show the image button again, so images can get attached
		updateViewState();
	}

	@Override
	public Parcelable onSaveInstanceState(@Nullable Parcelable superState) {
		SavedState state =
				new SavedState(superState == null ? EMPTY_STATE : superState);
		state.imageUris = imageUris;
		return state;
	}

	@Override
	@Nullable
	public Parcelable onRestoreInstanceState(Parcelable inState) {
		SavedState state = (SavedState) inState;
		if (!imageUris.isEmpty()) throw new AssertionError();
		if (state.imageUris != null) onNewUris(true, state.imageUris);
		return state.getSuperState();
	}

	@UiThread
	private void onError(@Nullable String errorMsg) {
		if (errorMsg == null) {
			errorMsg = imagePreview.getContext()
					.getString(R.string.image_attach_error);
		}
		Toast.makeText(textInput.getContext(), errorMsg, LENGTH_LONG).show();
		onCancel();
	}

	@Override
	public void onCancel() {
		textInput.clearText();
		attachmentManager.cancel();
		reset();
	}

	public void showImageOnboarding(Activity activity) {
		int color = resolveColorAttribute(activity, R.attr.colorControlNormal);
		new MaterialTapTargetPrompt.Builder(activity,
				R.style.OnboardingDialogTheme).setTarget(sendButton)
				.setPrimaryText(R.string.dialog_title_image_support)
				.setSecondaryText(R.string.dialog_message_image_support)
				.setBackgroundColour(getColor(activity, R.color.briar_primary))
				.setIcon(R.drawable.ic_image)
				.setIconDrawableColourFilter(color)
				.show();
	}

	private static class SavedState extends AbsSavedState {

		@Nullable
		private List<Uri> imageUris;

		private SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			//noinspection unchecked
			imageUris = in.readArrayList(Uri.class.getClassLoader());
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeList(imageUris);
		}

		public static final Creator<SavedState> CREATOR =
				new Creator<SavedState>() {
					@Override
					public SavedState createFromParcel(Parcel in) {
						return new SavedState(in);
					}

					@Override
					public SavedState[] newArray(int size) {
						return new SavedState[size];
					}
				};
	}

	@UiThread
	public interface AttachmentListener extends SendListener, LifecycleOwner {

		void onAttachImage(Intent intent);

		void onTooManyAttachments();
	}
}

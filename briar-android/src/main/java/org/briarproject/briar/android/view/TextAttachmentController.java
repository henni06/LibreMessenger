package org.briarproject.briar.android.view;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.view.AbsSavedState;
import android.support.v7.widget.AppCompatImageButton;
import android.widget.Toast;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.view.ImagePreview.ImagePreviewListener;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.ACTION_GET_CONTENT;
import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.CATEGORY_OPENABLE;
import static android.content.Intent.EXTRA_ALLOW_MULTIPLE;
import static android.os.Build.VERSION.SDK_INT;
import static android.support.v4.view.AbsSavedState.EMPTY_STATE;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_LONG;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

@UiThread
@NotNullByDefault
public class TextAttachmentController extends TextSendController
		implements ImagePreviewListener {

	private final AppCompatImageButton imageButton;
	private final ImagePreview imagePreview;

	private final AttachImageListener imageListener;

	private CharSequence textHint;
	private List<Uri> imageUris = emptyList();

	public TextAttachmentController(TextInputView v, ImagePreview imagePreview,
			SendListener listener, AttachImageListener imageListener) {
		super(v, listener, false);
		this.imageListener = imageListener;
		this.imagePreview = imagePreview;
		this.imagePreview.setImagePreviewListener(this);

		imageButton = v.findViewById(R.id.imageButton);
		imageButton.setOnClickListener(view -> onImageButtonClicked());

		textHint = textInput.getHint();

		// show image button
		showImageButton(true);
	}

	@Override
	public void onTextIsEmptyChanged(boolean isEmpty) {
		if (imageUris.isEmpty()) showImageButton(isEmpty);
	}

	@Override
	public void onSendEvent() {
		if (canSend()) {
			listener.onSendClick(textInput.getText(), imageUris);
			reset();
		}
	}

	@Override
	protected boolean canSendEmptyText() {
		return !imageUris.isEmpty();
	}

	private void onImageButtonClicked() {
		Intent intent = new Intent(SDK_INT >= 19 ?
				ACTION_OPEN_DOCUMENT : ACTION_GET_CONTENT);
		intent.addCategory(CATEGORY_OPENABLE);
		intent.setType("image/*");
		if (SDK_INT >= 18) intent.putExtra(EXTRA_ALLOW_MULTIPLE, true);
		requireNonNull(imageListener).onAttachImage(intent);
	}

	public void onImageReceived(@Nullable Intent resultData) {
		if (resultData == null) return;
		if (resultData.getData() != null) {
			imageUris = new ArrayList<>(1);
			imageUris.add(resultData.getData());
			onNewUris();
		} else if (SDK_INT >= 18 && resultData.getClipData() != null) {
			ClipData clipData = resultData.getClipData();
			imageUris = new ArrayList<>(clipData.getItemCount());
			for (int i = 0; i < clipData.getItemCount(); i++) {
				imageUris.add(clipData.getItemAt(i).getUri());
			}
			onNewUris();
		}
	}

	private void onNewUris() {
		if (imageUris.isEmpty()) return;
		showImageButton(false);
		textInput.setHint(R.string.image_caption_hint);
		imagePreview.showPreview(imageUris);
	}

	private void showImageButton(boolean showImageButton) {
		if (showImageButton) {
			imageButton.setVisibility(VISIBLE);
			sendButton.setEnabled(false);
			if (SDK_INT <= 15) {
				sendButton.setVisibility(INVISIBLE);
				imageButton.setEnabled(true);
			} else {
				sendButton.clearAnimation();
				sendButton.animate().alpha(0f).withEndAction(() -> {
					sendButton.setVisibility(INVISIBLE);
					imageButton.setEnabled(true);
				}).start();
				imageButton.clearAnimation();
				imageButton.animate().alpha(1f).start();
			}
		} else {
			sendButton.setVisibility(VISIBLE);
			// enable/disable buttons right away to allow fast sending
			sendButton.setEnabled(enabled);
			imageButton.setEnabled(false);
			if (SDK_INT <= 15) {
				imageButton.setVisibility(INVISIBLE);
			} else {
				sendButton.clearAnimation();
				sendButton.animate().alpha(1f).start();
				imageButton.clearAnimation();
				imageButton.animate().alpha(0f).withEndAction(() ->
						imageButton.setVisibility(INVISIBLE)
				).start();
			}
		}
	}

	private void reset() {
		// restore hint
		textInput.setHint(textHint);
		// hide image layout
		imagePreview.setVisibility(GONE);
		// reset image URIs
		imageUris = emptyList();
		// show the image button again, so images can get attached
		showImageButton(true);
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
		imageUris = requireNonNull(state.imageUris);
		onNewUris();
		return state.getSuperState();
	}

	@Override
	public void onUriError(Uri uri) {
		imageUris.remove(uri);
		imagePreview.removeUri(uri);
		if (imageUris.isEmpty()) onCancel();
		Toast.makeText(textInput.getContext(), R.string.image_attach_error,
				LENGTH_LONG).show();
	}

	@Override
	public void onCancel() {
		textInput.clearText();
		reset();
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

		public static final Parcelable.Creator<SavedState> CREATOR
				= new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	public interface AttachImageListener {
		void onAttachImage(Intent intent);
	}

}

package org.libreproject.libre.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.libreproject.libre.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.LinearLayoutCompat;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static java.util.Objects.requireNonNull;

public class CompositeSendButton extends FrameLayout {

	private final LinearLayoutCompat loTextActions;
	private final AppCompatImageButton sendButton, imageButton,speechButton;
	private final ImageView bombBadge;
	private final ProgressBar progressBar;

	private boolean hasImageSupport = false;

	public CompositeSendButton(@NonNull Context context,
			@Nullable AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater inflater = (LayoutInflater) requireNonNull(
				context.getSystemService(LAYOUT_INFLATER_SERVICE));
		inflater.inflate(R.layout.view_composite_send_button, this, true);

		sendButton = findViewById(R.id.sendButton);
		imageButton = findViewById(R.id.imageButton);
		speechButton=findViewById(R.id.speechButton);
		bombBadge = findViewById(R.id.bombBadge);
		progressBar = findViewById(R.id.progressBar);
		loTextActions=findViewById(R.id.loTextActions);
	}

	@Override
	public void setEnabled(boolean enabled) {
		setSendEnabled(enabled);
	}

	@Override
	public void setOnClickListener(@Nullable View.OnClickListener l) {
		setOnSendClickListener(l);
	}

	public void setOnSendClickListener(@Nullable OnClickListener l) {
		sendButton.setOnClickListener(l);
	}

	public void setSendEnabled(boolean enabled) {
		sendButton.setEnabled(enabled);
	}

	public void setOnImageClickListener(@Nullable OnClickListener l) {
		imageButton.setOnClickListener(l);
	}

	public void setOnSpeechTouchListener(@Nullable OnTouchListener l) {
		speechButton.setOnTouchListener(l);
	}

	/**
	 * By default, image support is disabled.
	 * Once you know that it is supported in the current context,
	 * call this method to enable it.
	 */
	public void setImagesSupported() {
		hasImageSupport = true;
		imageButton.setImageResource(R.drawable.ic_image);
	}

	public boolean hasImageSupport() {
		return hasImageSupport;
	}

	public void setBombVisible(boolean visible) {
		bombBadge.setVisibility(visible ? VISIBLE : INVISIBLE);
	}

	public void showImageButton(boolean showImageButton, boolean sendEnabled) {
		if (showImageButton) {
			loTextActions.setVisibility(VISIBLE);
			sendButton.setEnabled(false);
			sendButton.clearAnimation();
			sendButton.animate().alpha(0f).withEndAction(() -> {
				sendButton.setVisibility(INVISIBLE);
				imageButton.setEnabled(true);
			}).start();
			loTextActions.clearAnimation();
			loTextActions.animate().alpha(1f).start();
		} else {
			sendButton.setVisibility(VISIBLE);
			// enable/disable buttons right away to allow fast sending
			sendButton.setEnabled(sendEnabled);
			imageButton.setEnabled(false);
			sendButton.clearAnimation();
			sendButton.animate().alpha(1f).start();
			loTextActions.clearAnimation();
			loTextActions.animate().alpha(0f).withEndAction(() ->
					loTextActions.setVisibility(GONE)
			).start();
		}
	}

	public void showProgress(boolean show) {
		sendButton.setVisibility(show ? INVISIBLE : VISIBLE);
		loTextActions.setVisibility(show ? INVISIBLE : VISIBLE);
		progressBar.setVisibility(show ? VISIBLE : GONE);
	}

}

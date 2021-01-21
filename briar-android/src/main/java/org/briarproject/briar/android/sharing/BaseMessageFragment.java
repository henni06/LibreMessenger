package org.briarproject.briar.android.sharing;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.fragment.BaseFragment;
import org.briarproject.briar.android.view.LargeTextInputView;
import org.briarproject.briar.android.view.TextSendController;
import org.briarproject.briar.android.view.TextSendController.SendListener;
import org.briarproject.briar.api.media.AttachmentHeader;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public abstract class BaseMessageFragment extends BaseFragment
		implements SendListener {

	protected LargeTextInputView message;
	private TextSendController sendController;
	private MessageFragmentListener listener;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		listener = (MessageFragmentListener) context;
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {

		// inflate view
		View v = inflater.inflate(R.layout.fragment_message, container,
				false);
		message = v.findViewById(R.id.messageView);
		sendController = new TextSendController(message, this, true);
		message.setSendController(sendController);
		message.setMaxTextLength(listener.getMaximumTextLength());
		message.setButtonText(getString(getButtonText()));
		message.setHint(getHintText());

		return v;
	}

	protected void setTitle(int res) {
		listener.setTitle(res);
	}

	@StringRes
	protected abstract int getButtonText();
	@StringRes
	protected abstract int getHintText();

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (message.isKeyboardOpen()) message.hideSoftKeyboard();
				listener.onBackPressed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onSendClick(@Nullable String text,
			List<AttachmentHeader> headers) {
		// disable button to prevent accidental double actions
		sendController.setReady(false);
		message.hideSoftKeyboard();

		listener.onButtonClick(text);
	}

	@UiThread
	@NotNullByDefault
	public interface MessageFragmentListener {

		void onBackPressed();

		void setTitle(@StringRes int titleRes);

		void onButtonClick(@Nullable String text);

		int getMaximumTextLength();

	}

}

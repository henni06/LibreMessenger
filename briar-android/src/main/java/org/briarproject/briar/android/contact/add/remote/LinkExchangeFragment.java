package org.briarproject.briar.android.contact.add.remote;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.fragment.BaseFragment;

import java.util.regex.Matcher;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.widget.Toast.LENGTH_SHORT;
import static java.util.Objects.requireNonNull;
import static org.briarproject.bramble.api.contact.ContactManager.LINK_REGEX;
import static org.briarproject.briar.android.util.UiUtils.observeOnce;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class LinkExchangeFragment extends BaseFragment {

	private static final String TAG = LinkExchangeFragment.class.getName();

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private AddContactViewModel viewModel;

	private ClipboardManager clipboard;
	private TextInputLayout linkInputLayout;
	private TextInputEditText linkInput;

	@Override
	public String getUniqueTag() {
		return TAG;
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
		if (getActivity() == null || getContext() == null) return null;

		viewModel = ViewModelProviders.of(getActivity(), viewModelFactory)
				.get(AddContactViewModel.class);

		View v = inflater.inflate(R.layout.fragment_link_exchange,
				container, false);

		linkInputLayout = v.findViewById(R.id.linkInputLayout);
		linkInput = v.findViewById(R.id.linkInput);

		clipboard = (ClipboardManager) requireNonNull(
				getContext().getSystemService(CLIPBOARD_SERVICE));

		Button pasteButton = v.findViewById(R.id.pasteButton);
		pasteButton.setOnClickListener(view -> {
			ClipData clipData = clipboard.getPrimaryClip();
			if (clipData != null && clipData.getItemCount() > 0)
				linkInput.setText(clipData.getItemAt(0).getText());
		});

		observeOnce(viewModel.getOurLink(), this, this::onOwnLinkLoaded);

		return v;
	}

	private void onOwnLinkLoaded(String link) {
		View v = requireNonNull(getView());

		TextView linkView = v.findViewById(R.id.linkView);
		linkView.setText(link);

		Button copyButton = v.findViewById(R.id.copyButton);
		ClipData clip = ClipData.newPlainText(
				getString(R.string.link_clip_label), link);
		copyButton.setOnClickListener(view -> {
			clipboard.setPrimaryClip(clip);
			Toast.makeText(getContext(), R.string.link_copied_toast,
					LENGTH_SHORT).show();
		});
		copyButton.setEnabled(true);

		Button shareButton = v.findViewById(R.id.shareButton);
		shareButton.setOnClickListener(view ->
				IntentBuilder.from(requireActivity())
						.setText(link)
						.setType("text/plain")
						.startChooser());
		shareButton.setEnabled(true);

		Button continueButton = v.findViewById(R.id.addButton);
		continueButton.setOnClickListener(view -> onContinueButtonClicked());
		continueButton.setEnabled(true);
	}

	/**
	 * Requires {@link AddContactViewModel#getOurLink()} to be loaded.
	 */
	@Nullable
	private String getEnteredLinkOrNull() {
		CharSequence link = linkInput.getText();
		if (link == null || link.length() == 0) {
			linkInputLayout.setError(getString(R.string.missing_link));
			linkInput.requestFocus();
			return null;
		}

		Matcher matcher = LINK_REGEX.matcher(link);
		if (matcher.find()) {
			String linkWithoutSchema = matcher.group(2);
			// Check also if this is our own link. This was loaded already,
			// because it enables the Continue button which is the only caller.
			if (("briar://" + linkWithoutSchema)
					.equals(viewModel.getOurLink().getValue())) {
				linkInputLayout.setError(getString(R.string.own_link_error));
				linkInput.requestFocus();
				return null;
			}
			linkInputLayout.setError(null);
			return linkWithoutSchema;
		}
		linkInputLayout.setError(getString(R.string.invalid_link));
		linkInput.requestFocus();
		return null;
	}

	private void onContinueButtonClicked() {
		String link = getEnteredLinkOrNull();
		if (link == null) return;  // invalid link

		viewModel.setRemoteContactLink(link);
		viewModel.onRemoteLinkEntered();
	}

}

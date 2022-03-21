package org.libreproject.libre.android.blog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.libre.R;
import org.libreproject.libre.android.activity.BaseActivity;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class RssFeedImportFailedDialogFragment extends DialogFragment {
	final static String TAG = RssFeedImportFailedDialogFragment.class.getName();

	@Inject
	ViewModelProvider.Factory viewModelFactory;
	private RssFeedViewModel viewModel;

	private static final String ARG_URL = "url";

	static RssFeedImportFailedDialogFragment newInstance(String retryUrl) {
		Bundle args = new Bundle();
		args.putString(ARG_URL, retryUrl);
		RssFeedImportFailedDialogFragment f =
				new RssFeedImportFailedDialogFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public void onAttach(Context ctx) {
		super.onAttach(ctx);
		((BaseActivity) requireActivity()).getActivityComponent().inject(this);

		viewModel = new ViewModelProvider(requireActivity(), viewModelFactory)
				.get(RssFeedViewModel.class);
	}

	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		AlertDialog.Builder builder =
				new AlertDialog.Builder(requireActivity(),
						R.style.LibreDialogTheme);
		builder.setMessage(R.string.blogs_rss_feeds_import_error);
		builder.setNegativeButton(R.string.cancel, null);
		builder.setPositiveButton(R.string.try_again_button, (dialog, which) ->
				viewModel.importFeed(requireArguments().getString(ARG_URL)));

		return builder.create();
	}
}

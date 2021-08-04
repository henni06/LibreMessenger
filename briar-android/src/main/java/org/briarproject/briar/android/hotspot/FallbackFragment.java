package org.briarproject.briar.android.hotspot;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.fragment.BaseFragment;
import org.briarproject.briar.android.util.ActivityLaunchers.CreateDocumentAdvanced;

import java.util.List;

import javax.inject.Inject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.EXTRA_STREAM;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.pm.PackageManager.MATCH_DEFAULT_ONLY;
import static android.os.Build.VERSION.SDK_INT;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.transition.TransitionManager.beginDelayedTransition;
import static org.briarproject.briar.android.AppModule.getAndroidComponent;
import static org.briarproject.briar.android.hotspot.HotspotViewModel.getApkFileName;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class FallbackFragment extends BaseFragment {

	public static final String TAG = FallbackFragment.class.getName();

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private HotspotViewModel viewModel;
	private final ActivityResultLauncher<String> launcher =
			registerForActivityResult(new CreateDocumentAdvanced(),
					this::onDocumentCreated);
	private Button fallbackButton;
	private ProgressBar progressBar;

	@Override
	public String getUniqueTag() {
		return TAG;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		FragmentActivity activity = requireActivity();
		getAndroidComponent(activity).inject(this);
		viewModel = new ViewModelProvider(activity, viewModelFactory)
				.get(HotspotViewModel.class);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		return inflater
				.inflate(R.layout.fragment_hotspot_fallback, container, false);
	}

	@Override
	public void onViewCreated(View v, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		fallbackButton = v.findViewById(R.id.fallbackButton);
		progressBar = v.findViewById(R.id.progressBar);
		fallbackButton.setOnClickListener(view -> {
			beginDelayedTransition((ViewGroup) v);
			fallbackButton.setVisibility(INVISIBLE);
			progressBar.setVisibility(VISIBLE);

			if (SDK_INT >= 19) launcher.launch(getApkFileName());
			else viewModel.exportApk();
		});
		viewModel.getSavedApkToUri().observeEvent(this, this::shareUri);
	}

	private void onDocumentCreated(@Nullable Uri uri) {
		showButton();
		if (uri != null) viewModel.exportApk(uri);
	}

	private void showButton() {
		beginDelayedTransition((ViewGroup) requireView());
		fallbackButton.setVisibility(VISIBLE);
		progressBar.setVisibility(INVISIBLE);
	}

	void shareUri(Uri uri) {
		Intent i = new Intent(ACTION_SEND);
		i.putExtra(EXTRA_STREAM, uri);
		i.setType("*/*"); // gives us all sharing options
		i.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
		Context ctx = requireContext();
		if (SDK_INT <= 19) {
			// Workaround for Android bug:
			// ctx.grantUriPermission also needed for Android 4
			List<ResolveInfo> resInfoList = ctx.getPackageManager()
					.queryIntentActivities(i, MATCH_DEFAULT_ONLY);
			for (ResolveInfo resolveInfo : resInfoList) {
				String packageName = resolveInfo.activityInfo.packageName;
				ctx.grantUriPermission(packageName, uri,
						FLAG_GRANT_READ_URI_PERMISSION);
			}
		}
		startActivity(Intent.createChooser(i, null));
	}

}

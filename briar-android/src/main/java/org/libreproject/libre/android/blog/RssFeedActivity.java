package org.libreproject.libre.android.blog;

import android.os.Bundle;
import android.widget.Toast;

import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.libre.R;
import org.libreproject.libre.android.activity.ActivityComponent;
import org.libreproject.libre.android.activity.BriarActivity;
import org.libreproject.libre.android.fragment.BaseFragment.BaseFragmentListener;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import static org.libreproject.libre.android.blog.RssFeedViewModel.ImportResult.EXISTS;
import static org.libreproject.libre.android.blog.RssFeedViewModel.ImportResult.FAILED;
import static org.libreproject.libre.android.blog.RssFeedViewModel.ImportResult.IMPORTED;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class RssFeedActivity extends BriarActivity
		implements BaseFragmentListener {

	@Inject
	ViewModelProvider.Factory viewModelFactory;
	private RssFeedViewModel viewModel;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);

		viewModel = new ViewModelProvider(this, viewModelFactory)
				.get(RssFeedViewModel.class);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_fragment_container);

		if (savedInstanceState == null) {
			showInitialFragment(RssFeedManageFragment.newInstance());
		}

		viewModel.getImportResult().observeEvent(this, this::onImportResult);
	}

	private void onImportResult(RssFeedViewModel.ImportResult result) {
		if (result == IMPORTED) {
			FragmentManager fm = getSupportFragmentManager();
			if (fm.findFragmentByTag(RssFeedImportFragment.TAG) != null) {
				onBackPressed();
			}
		} else if (result == FAILED) {
			String url = viewModel.getUrlFailedImport();
			if (url == null) {
				throw new AssertionError();
			}
			RssFeedImportFailedDialogFragment dialog =
					RssFeedImportFailedDialogFragment.newInstance(url);
			dialog.show(getSupportFragmentManager(),
					RssFeedImportFailedDialogFragment.TAG);
		} else if (result == EXISTS) {
			Toast.makeText(this, R.string.blogs_rss_feeds_import_exists,
					Toast.LENGTH_LONG).show();
		}
	}
}

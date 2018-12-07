package org.briarproject.briar.android.conversation;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Transition;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.widget.TextView;

import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.activity.BriarActivity;
import org.briarproject.briar.android.view.PullDownLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import static android.content.Intent.ACTION_CREATE_DOCUMENT;
import static android.content.Intent.CATEGORY_OPENABLE;
import static android.content.Intent.EXTRA_TITLE;
import static android.graphics.Color.TRANSPARENT;
import static android.os.Build.VERSION.SDK_INT;
import static android.support.design.widget.Snackbar.LENGTH_LONG;
import static android.view.View.GONE;
import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
import static android.view.View.VISIBLE;
import static android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
import static java.util.Objects.requireNonNull;
import static org.briarproject.briar.android.activity.RequestCodes.REQUEST_SAVE_ATTACHMENT;
import static org.briarproject.briar.android.util.UiUtils.formatDateAbsolute;

public class ImageActivity extends BriarActivity
		implements PullDownLayout.Callback, OnGlobalLayoutListener {

	final static String ATTACHMENTS = "attachments";
	final static String ATTACHMENT_POSITION = "position";
	final static String NAME = "name";
	final static String DATE = "date";

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private ImageViewModel viewModel;
	private PullDownLayout layout;
	private AppBarLayout appBarLayout;
	private ViewPager viewPager;
	private List<AttachmentItem> attachments;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);

		// Transitions
		supportPostponeEnterTransition();
		Window window = getWindow();
		if (SDK_INT >= 21) {
			Transition transition = new Fade();
			setSceneTransitionAnimation(transition, null, transition);
		}

		// get View Model
		viewModel = ViewModelProviders.of(this, viewModelFactory)
				.get(ImageViewModel.class);
		viewModel.getSaveState().observe(this, this::onImageSaveStateChanged);

		// inflate layout
		setContentView(R.layout.activity_image);
		layout = findViewById(R.id.layout);
		layout.getBackground().setAlpha(255);
		layout.setCallback(this);
		layout.getViewTreeObserver().addOnGlobalLayoutListener(this);

		// Status Bar
		if (SDK_INT >= 21) {
			window.setStatusBarColor(TRANSPARENT);
		} else if (SDK_INT >= 19) {
			// we can't make the status bar transparent, but translucent
			window.addFlags(FLAG_TRANSLUCENT_STATUS);
		}

		// Toolbar
		appBarLayout = findViewById(R.id.appBarLayout);
		Toolbar toolbar = requireNonNull(setUpCustomToolbar(true));
		TextView contactName = toolbar.findViewById(R.id.contactName);
		TextView dateView = toolbar.findViewById(R.id.dateView);

		// Intent Extras
		Intent i = getIntent();
		attachments = i.getParcelableArrayListExtra(ATTACHMENTS);
		int position = i.getIntExtra(ATTACHMENT_POSITION, -1);
		if (position == -1) throw new IllegalStateException();
		String name = i.getStringExtra(NAME);
		long time = i.getLongExtra(DATE, 0);
		String date = formatDateAbsolute(this, time);
		contactName.setText(name);
		dateView.setText(date);

		// Set up image ViewPager
		viewPager = findViewById(R.id.viewPager);
		ImagePagerAdapter pagerAdapter =
				new ImagePagerAdapter(getSupportFragmentManager());
		viewPager.setAdapter(pagerAdapter);
		viewPager.setCurrentItem(position);

		if (SDK_INT >= 16) {
			viewModel.getOnImageClicked().observe(this, this::onPhotoClicked);
			window.getDecorView().setSystemUiVisibility(
					SYSTEM_UI_FLAG_LAYOUT_STABLE |
							SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.image_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
			case R.id.action_save_image:
				showSaveImageDialog();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onGlobalLayout() {
		viewModel.setToolbarPosition(
				appBarLayout.getTop(), appBarLayout.getBottom()
		);
		if (SDK_INT >= 16) {
			layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
		}
	}

	@Override
	protected void onActivityResult(int request, int result, Intent data) {
		super.onActivityResult(request, result, data);
		if (request == REQUEST_SAVE_ATTACHMENT && result == RESULT_OK) {
			viewModel.saveImage(getVisibleAttachment(), data.getData());
		}
	}

	@Override
	public void onPullStart() {
		appBarLayout.animate()
				.alpha(0f)
				.start();
	}

	@Override
	public void onPull(float progress) {
		layout.getBackground().setAlpha(Math.round((1 - progress) * 255));
	}

	@Override
	public void onPullCancel() {
		appBarLayout.animate()
				.alpha(1f)
				.start();
	}

	@Override
	public void onPullComplete() {
		supportFinishAfterTransition();
	}

	private void onPhotoClicked(@Nullable Boolean clicked) {
		if (clicked != null&& clicked && SDK_INT >= 16) {
			toggleSystemUi();
			viewModel.onOnImageClickSeen();
		}
	}

	@RequiresApi(api = 16)
	private void toggleSystemUi() {
		View decorView = getWindow().getDecorView();
		if (appBarLayout.getVisibility() == VISIBLE) {
			hideSystemUi(decorView);
		} else {
			showSystemUi(decorView);
		}
	}

	@RequiresApi(api = 16)
	private void hideSystemUi(View decorView) {
		decorView.setSystemUiVisibility(SYSTEM_UI_FLAG_FULLSCREEN |
				SYSTEM_UI_FLAG_LAYOUT_STABLE | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
		);
		appBarLayout.animate()
				.translationYBy(-1 * appBarLayout.getHeight())
				.alpha(0f)
				.withEndAction(() -> appBarLayout.setVisibility(GONE))
				.start();
	}

	@RequiresApi(api = 16)
	private void showSystemUi(View decorView) {
		decorView.setSystemUiVisibility(
				SYSTEM_UI_FLAG_LAYOUT_STABLE | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
		);
		appBarLayout.animate()
				.translationYBy(appBarLayout.getHeight())
				.alpha(1f)
				.withStartAction(() -> appBarLayout.setVisibility(VISIBLE))
				.start();
	}

	private void showSaveImageDialog() {
		OnClickListener okListener = (dialog, which) -> {
			if (SDK_INT >= 19) {
				Intent intent = getCreationIntent();
				startActivityForResult(intent, REQUEST_SAVE_ATTACHMENT);
			} else {
				viewModel.saveImage(getVisibleAttachment());
			}
		};
		Builder builder = new Builder(this, R.style.BriarDialogTheme);
		builder.setTitle(getString(R.string.dialog_title_save_image));
		builder.setMessage(getString(R.string.dialog_message_save_image));
		Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_security);
		DrawableCompat.setTint(requireNonNull(icon),
				ContextCompat.getColor(this, R.color.color_primary));
		builder.setIcon(icon);
		builder.setPositiveButton(R.string.save_image, okListener);
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	@RequiresApi(api = 19)
	private Intent getCreationIntent() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
				Locale.getDefault());
		String fileName = sdf.format(new Date());
		Intent intent = new Intent(ACTION_CREATE_DOCUMENT);
		intent.addCategory(CATEGORY_OPENABLE);
		intent.setType(getVisibleAttachment().getMimeType());
		intent.putExtra(EXTRA_TITLE, fileName);
		return intent;
	}

	private void onImageSaveStateChanged(@Nullable Boolean error) {
		if (error == null) return;
		int stringRes = error ?
				R.string.save_image_error : R.string.save_image_success;
		int colorRes = error ?
				R.color.briar_red : R.color.briar_primary;
		Snackbar s = Snackbar.make(layout, stringRes, LENGTH_LONG);
		s.getView().setBackgroundResource(colorRes);
		s.show();
		viewModel.onSaveStateSeen();
	}

	AttachmentItem getVisibleAttachment() {
		return attachments.get(viewPager.getCurrentItem());
	}

	private class ImagePagerAdapter extends FragmentStatePagerAdapter {

		private ImagePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return ImageFragment.newInstance(attachments.get(position));
		}

		@Override
		public int getCount() {
			return attachments.size();
		}

	}

}

package org.libreproject.libre.android.blog;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ProgressBar;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.identity.IdentityManager;
import org.libreproject.bramble.api.identity.LocalAuthor;
import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.R;
import org.libreproject.libre.android.activity.ActivityComponent;
import org.libreproject.libre.android.activity.BriarActivity;
import org.libreproject.libre.android.view.TextInputView;
import org.libreproject.libre.android.view.TextSendController;
import org.libreproject.libre.android.view.TextSendController.SendListener;
import org.libreproject.libre.api.android.AndroidNotificationManager;
import org.libreproject.libre.api.attachment.AttachmentHeader;
import org.libreproject.libre.api.blog.BlogManager;
import org.libreproject.libre.api.blog.BlogPost;
import org.libreproject.libre.api.blog.BlogPostFactory;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static java.util.logging.Level.WARNING;
import static org.libreproject.bramble.util.LogUtils.logException;
import static org.libreproject.bramble.util.StringUtils.isNullOrEmpty;
import static org.libreproject.libre.android.view.TextSendController.SendState;
import static org.libreproject.libre.android.view.TextSendController.SendState.SENT;
import static org.libreproject.libre.api.blog.BlogConstants.MAX_BLOG_POST_TEXT_LENGTH;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class WriteBlogPostActivity extends BriarActivity
		implements SendListener {

	private static final Logger LOG =
			Logger.getLogger(WriteBlogPostActivity.class.getName());

	@Inject
	AndroidNotificationManager notificationManager;

	private TextInputView input;
	private ProgressBar progressBar;

	// Fields that are accessed from background threads must be volatile
	private volatile GroupId groupId;
	@Inject
	volatile IdentityManager identityManager;
	@Inject
	volatile BlogPostFactory blogPostFactory;
	@Inject
	volatile BlogManager blogManager;

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);

		Intent i = getIntent();
		byte[] b = i.getByteArrayExtra(GROUP_ID);
		if (b == null) throw new IllegalStateException("No Group in intent.");
		groupId = new GroupId(b);

		setContentView(R.layout.activity_write_blog_post);

		input = findViewById(R.id.textInput);
		TextSendController sendController =
				new TextSendController(input, this, false);
		input.setSendController(sendController);
		input.setMaxTextLength(MAX_BLOG_POST_TEXT_LENGTH);
		input.setReady(true);

		progressBar = findViewById(R.id.progressBar);
	}

	@Override
	public void onStart() {
		super.onStart();
		notificationManager.blockNotification(groupId);
	}

	@Override
	public void onStop() {
		super.onStop();
		notificationManager.unblockNotification(groupId);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	public LiveData<SendState> onSendClick(@Nullable String text,
			List<AttachmentHeader> headers, long expectedAutoDeleteTimer) {
		if (isNullOrEmpty(text)) throw new AssertionError();

		// hide publish button, show progress bar
		input.hideSoftKeyboard();
		input.setVisibility(GONE);
		progressBar.setVisibility(VISIBLE);

		storePost(text);
		return new MutableLiveData<>(SENT);
	}

	private void storePost(String text) {
		runOnDbThread(() -> {
			long timestamp = System.currentTimeMillis();
			try {
				LocalAuthor author = identityManager.getLocalAuthor();
				BlogPost p = blogPostFactory
						.createBlogPost(groupId, timestamp, null, author, text);
				blogManager.addLocalPost(p);
				postPublished();
			} catch (DbException | GeneralSecurityException
					| FormatException e) {
				logException(LOG, WARNING, e);
				postFailedToPublish();
			}
		});
	}

	private void postPublished() {
		runOnUiThreadUnlessDestroyed(() -> {
			setResult(RESULT_OK);
			supportFinishAfterTransition();
		});
	}

	private void postFailedToPublish() {
		runOnUiThreadUnlessDestroyed(() -> {
			// hide progress bar, show publish button
			progressBar.setVisibility(GONE);
			input.setVisibility(VISIBLE);
			// TODO show error
		});
	}
}

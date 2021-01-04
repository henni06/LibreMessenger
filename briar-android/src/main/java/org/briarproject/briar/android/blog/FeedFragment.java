package org.briarproject.briar.android.blog;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.blog.FeedController.FeedListener;
import org.briarproject.briar.android.controller.handler.UiResultExceptionHandler;
import org.briarproject.briar.android.fragment.BaseFragment;
import org.briarproject.briar.android.util.BriarSnackbarBuilder;
import org.briarproject.briar.android.view.BriarRecyclerView;
import org.briarproject.briar.api.blog.Blog;
import org.briarproject.briar.api.blog.BlogPostHeader;

import java.util.Collection;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.LinearLayoutManager;

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static com.google.android.material.snackbar.Snackbar.LENGTH_LONG;
import static org.briarproject.briar.android.activity.BriarActivity.GROUP_ID;
import static org.briarproject.briar.android.activity.RequestCodes.REQUEST_WRITE_BLOG_POST;

@UiThread
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class FeedFragment extends BaseFragment implements
		OnBlogPostClickListener, FeedListener {

	public final static String TAG = FeedFragment.class.getName();
	private static final Logger LOG = Logger.getLogger(TAG);

	@Inject
	FeedController feedController;

	private BlogPostAdapter adapter;
	private LinearLayoutManager layoutManager;
	private BriarRecyclerView list;
	@Nullable
	private Blog personalBlog;
	@Nullable
	private Parcelable layoutManagerState;

	public static FeedFragment newInstance() {
		FeedFragment f = new FeedFragment();

		Bundle args = new Bundle();
		f.setArguments(args);

		return f;
	}

	@Override
	public void injectFragment(ActivityComponent component) {
		component.inject(this);
		feedController.setFeedListener(this);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		requireActivity().setTitle(R.string.blogs_button);

		View v = inflater.inflate(R.layout.fragment_blog, container, false);

		adapter =
				new BlogPostAdapter(getActivity(), this, getFragmentManager());

		layoutManager = new LinearLayoutManager(getActivity());
		list = v.findViewById(R.id.postList);
		list.setLayoutManager(layoutManager);
		list.setAdapter(adapter);
		list.setEmptyImage(R.drawable.ic_empty_state_blog);
		list.setEmptyText(R.string.blogs_feed_empty_state);
		list.setEmptyAction(R.string.blogs_feed_empty_state_action);

		if (savedInstanceState != null) {
			layoutManagerState =
					savedInstanceState.getParcelable("layoutManager");
		}

		return v;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode,
			@Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// The BlogPostAddedEvent arrives when the controller is not listening
		if (requestCode == REQUEST_WRITE_BLOG_POST && resultCode == RESULT_OK) {
			showSnackBar(R.string.blogs_blog_post_created);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		feedController.onStart();
		list.startPeriodicUpdate();
		loadPersonalBlog();
		loadBlogPosts(false);
	}

	@Override
	public void onStop() {
		super.onStop();
		feedController.onStop();
		adapter.clear();
		list.showProgressBar();
		list.stopPeriodicUpdate();
		// TODO save list position in database/preferences?
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		feedController.unsetFeedListener(this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (layoutManager != null) {
			layoutManagerState = layoutManager.onSaveInstanceState();
			outState.putParcelable("layoutManager", layoutManagerState);
		}
	}

	private void loadPersonalBlog() {
		feedController.loadPersonalBlog(
				new UiResultExceptionHandler<Blog, DbException>(this) {
					@Override
					public void onResultUi(Blog b) {
						personalBlog = b;
					}

					@Override
					public void onExceptionUi(DbException exception) {
						handleException(exception);
					}
				});
	}

	private void loadBlogPosts(boolean clear) {
		int revision = adapter.getRevision();
		feedController.loadBlogPosts(
				new UiResultExceptionHandler<Collection<BlogPostItem>, DbException>(
						this) {
					@Override
					public void onResultUi(Collection<BlogPostItem> posts) {
						if (revision == adapter.getRevision()) {
							adapter.incrementRevision();
							if (clear) adapter.setItems(posts);
							else adapter.addAll(posts);
							if (posts.isEmpty()) list.showData();
							if (layoutManagerState == null) {
								list.scrollToPosition(0);  // Scroll to the top
							} else {
								layoutManager.onRestoreInstanceState(
										layoutManagerState);
							}
						} else {
							LOG.info("Concurrent update, reloading");
							loadBlogPosts(clear);
						}
					}

					@Override
					public void onExceptionUi(DbException exception) {
						handleException(exception);
					}
				});
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.blogs_feed_actions, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (personalBlog == null) return false;
		switch (item.getItemId()) {
			case R.id.action_write_blog_post:
				Intent i1 =
						new Intent(getActivity(), WriteBlogPostActivity.class);
				i1.putExtra(GROUP_ID, personalBlog.getId().getBytes());
				startActivityForResult(i1, REQUEST_WRITE_BLOG_POST);
				return true;
			case R.id.action_rss_feeds_import:
				Intent i2 =
						new Intent(getActivity(), RssFeedImportActivity.class);
				startActivity(i2);
				return true;
			case R.id.action_rss_feeds_manage:
				Intent i3 =
						new Intent(getActivity(), RssFeedManageActivity.class);
				i3.putExtra(GROUP_ID, personalBlog.getId().getBytes());
				startActivity(i3);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBlogPostAdded(BlogPostHeader header, boolean local) {
		feedController.loadBlogPost(header,
				new UiResultExceptionHandler<BlogPostItem, DbException>(
						this) {
					@Override
					public void onResultUi(BlogPostItem post) {
						adapter.incrementRevision();
						adapter.add(post);
						if (local) {
							showSnackBar(R.string.blogs_blog_post_created);
						} else {
							showSnackBar(R.string.blogs_blog_post_received);
						}
					}

					@Override
					public void onExceptionUi(DbException exception) {
						handleException(exception);
					}
				}
		);
	}

	@Override
	public void onBlogPostClick(BlogPostItem post) {
		FeedPostFragment f =
				FeedPostFragment.newInstance(post.getGroupId(), post.getId());
		showNextFragment(f);
	}

	@Override
	public void onAuthorClick(BlogPostItem post) {
		Intent i = new Intent(getContext(), BlogActivity.class);
		i.putExtra(GROUP_ID, post.getGroupId().getBytes());
		i.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
		getContext().startActivity(i);
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

	private void showSnackBar(int stringRes) {
		int firstVisible =
				layoutManager.findFirstCompletelyVisibleItemPosition();
		int lastVisible = layoutManager.findLastCompletelyVisibleItemPosition();
		int count = adapter.getItemCount();
		boolean scroll = count > (lastVisible - firstVisible + 1);

		BriarSnackbarBuilder sb = new BriarSnackbarBuilder();
		if (scroll) {
			sb.setAction(R.string.blogs_blog_post_scroll_to,
					v -> list.smoothScrollToPosition(0));
		}
		sb.make(list, stringRes, LENGTH_LONG).show();
	}

	@Override
	public void onBlogAdded() {
		loadBlogPosts(false);
	}

	@Override
	public void onBlogRemoved() {
		loadBlogPosts(true);
	}

}

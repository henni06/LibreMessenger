package org.briarproject.briar.android.blog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.briar.R;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
class BlogPostAdapter extends ListAdapter<BlogPostItem, BlogPostViewHolder> {

	private final OnBlogPostClickListener listener;
	@Nullable
	private final FragmentManager fragmentManager;

	BlogPostAdapter(OnBlogPostClickListener listener,
			@Nullable FragmentManager fragmentManager) {
		super(new DiffUtil.ItemCallback<BlogPostItem>() {
			@Override
			public boolean areItemsTheSame(BlogPostItem a, BlogPostItem b) {
				return a.getId().equals(b.getId());
			}

			@Override
			public boolean areContentsTheSame(BlogPostItem a, BlogPostItem b) {
				return a.isRead() == b.isRead();
			}
		});
		this.listener = listener;
		this.fragmentManager = fragmentManager;
	}

	@Override
	public BlogPostViewHolder onCreateViewHolder(ViewGroup parent,
			int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(
				R.layout.list_item_blog_post, parent, false);
		return new BlogPostViewHolder(v, false, listener, fragmentManager);
	}

	@Override
	public void onBindViewHolder(BlogPostViewHolder ui, int position) {
		ui.bindItem(getItem(position));
	}

}

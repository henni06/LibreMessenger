package org.briarproject.briar.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.briarproject.bramble.api.identity.Author;
import org.briarproject.briar.R;
import org.briarproject.briar.android.conversation.glide.GlideApp;
import org.briarproject.briar.android.util.UiUtils;
import org.briarproject.briar.api.identity.AuthorInfo;

import javax.annotation.Nullable;

import androidx.annotation.DimenRes;
import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;
import de.hdodenhof.circleimageview.CircleImageView;
import im.delight.android.identicons.IdenticonDrawable;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.graphics.Typeface.BOLD;
import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static org.briarproject.briar.android.util.UiUtils.getContactDisplayName;
import static org.briarproject.briar.android.util.UiUtils.resolveAttribute;
import static org.briarproject.briar.api.identity.AuthorInfo.Status.NONE;
import static org.briarproject.briar.api.identity.AuthorInfo.Status.OURSELVES;

@UiThread
public class AuthorView extends ConstraintLayout {

	public static final int NORMAL = 0;
	public static final int REBLOGGER = 1;
	public static final int COMMENTER = 2;
	public static final int LIST = 3;
	public static final int RSS_FEED = 4;
	public static final int RSS_FEED_REBLOGGED = 5;

	private final CircleImageView avatar;
	private final ImageView avatarIcon;
	private final TextView authorName;
	private final Typeface authorNameTypeface;
	private final TextView date;
	private final TrustIndicatorView trustIndicator;

	public AuthorView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.author_view, this, true);

		avatar = findViewById(R.id.avatar);
		avatarIcon = findViewById(R.id.avatarIcon);
		authorName = findViewById(R.id.authorName);
		authorNameTypeface = authorName.getTypeface();
		date = findViewById(R.id.dateView);
		trustIndicator = findViewById(R.id.trustIndicator);

		TypedArray attributes =
				context.obtainStyledAttributes(attrs, R.styleable.AuthorView);
		int persona = attributes.getInteger(R.styleable.AuthorView_persona, 0);
		setPersona(persona);
		attributes.recycle();
	}

	public AuthorView(Context context) {
		this(context, null);
	}

	public void setAuthor(Author author, AuthorInfo authorInfo) {
		authorName
				.setText(getContactDisplayName(author, authorInfo.getAlias()));
		IdenticonDrawable identicon =
				new IdenticonDrawable(author.getId().getBytes());
		if (authorInfo.getAvatarHeader() == null) {
			avatar.setImageDrawable(identicon);
		} else {
			GlideApp.with(avatar)
					.load(authorInfo.getAvatarHeader())
					.diskCacheStrategy(DiskCacheStrategy.NONE)
					.error(identicon)
					.transition(withCrossFade())
					.into(avatar)
					.waitForLayout();
		}

		if (authorInfo.getStatus() != NONE) {
			trustIndicator.setTrustLevel(authorInfo.getStatus());
			trustIndicator.setVisibility(VISIBLE);
		} else {
			trustIndicator.setVisibility(GONE);
		}

		if (authorInfo.getStatus() == OURSELVES) {
			authorName.setTypeface(authorNameTypeface, BOLD);
		} else {
			authorName.setTypeface(authorNameTypeface, NORMAL);
		}

		invalidate();
		requestLayout();
	}

	public void setDate(long date) {
		this.date.setText(UiUtils.formatDate(getContext(), date));

		invalidate();
		requestLayout();
	}

	public void setAuthorClickable(OnClickListener listener) {
		setClickable(true);
		int res =
				resolveAttribute(getContext(), R.attr.selectableItemBackground);
		setBackgroundResource(res);
		setOnClickListener(listener);
	}

	public void setAuthorNotClickable() {
		setClickable(false);
		setBackgroundResource(0);
		setOnClickListener(null);
	}

	/**
	 * Styles this view for a different persona.
	 * <p>
	 * Attention: RSS_FEED and RSS_FEED_REBLOGGED change the avatar
	 * and override the one set by
	 * {@link AuthorView#setAuthor(Author, AuthorInfo)}.
	 */
	public void setPersona(int persona) {
		switch (persona) {
			case NORMAL:
				avatarIcon.setVisibility(INVISIBLE);
				date.setVisibility(VISIBLE);
				setAvatarSize(R.dimen.blogs_avatar_normal_size);
				setTextSize(authorName, R.dimen.text_size_small);
				break;
			case REBLOGGER:
				avatarIcon.setVisibility(VISIBLE);
				date.setVisibility(VISIBLE);
				setAvatarSize(R.dimen.blogs_avatar_normal_size);
				setTextSize(authorName, R.dimen.text_size_small);
				break;
			case COMMENTER:
				avatarIcon.setVisibility(INVISIBLE);
				date.setVisibility(VISIBLE);
				setAvatarSize(R.dimen.blogs_avatar_comment_size);
				setTextSize(authorName, R.dimen.text_size_tiny);
				break;
			case LIST:
				avatarIcon.setVisibility(INVISIBLE);
				date.setVisibility(GONE);
				setAvatarSize(R.dimen.listitem_picture_size_small);
				setTextSize(authorName, R.dimen.text_size_medium);
				break;
			case RSS_FEED:
				avatarIcon.setVisibility(INVISIBLE);
				date.setVisibility(VISIBLE);
				avatar.setImageResource(R.drawable.ic_rss_feed);
				setAvatarSize(R.dimen.blogs_avatar_normal_size);
				setTextSize(authorName, R.dimen.text_size_small);
				break;
			case RSS_FEED_REBLOGGED:
				avatarIcon.setVisibility(INVISIBLE);
				date.setVisibility(VISIBLE);
				avatar.setImageResource(R.drawable.ic_rss_feed);
				setAvatarSize(R.dimen.blogs_avatar_comment_size);
				setTextSize(authorName, R.dimen.text_size_tiny);
				break;
		}
	}

	private void setAvatarSize(@DimenRes int res) {
		LayoutParams params = (LayoutParams) avatar.getLayoutParams();
		int size = getResources().getDimensionPixelSize(res);
		params.height = size;
		params.width = size;
		avatar.setLayoutParams(params);
	}

	private void setTextSize(TextView v, @DimenRes int res) {
		float textSize = getResources().getDimensionPixelSize(res);
		v.setTextSize(COMPLEX_UNIT_PX, textSize);
	}

}

package org.libreproject.libre.android.conversation;

import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.R;
import org.libreproject.libre.android.attachment.AttachmentItem;
import org.libreproject.libre.android.view.EmojiTextInputView;

import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool;

import static androidx.constraintlayout.widget.ConstraintSet.WRAP_CONTENT;
import static androidx.core.content.ContextCompat.getColor;
import static androidx.core.widget.ImageViewCompat.setImageTintList;

@UiThread
@NotNullByDefault
class ConversationMessageViewHolder extends ConversationItemViewHolder {
	private com.vanniktech.emoji.EmojiTextView text;
	private boolean playing;
	private final ImageAdapter adapter;
	private final ViewGroup statusLayout;
	private final int timeColor, timeColorBubble;
	private final ConstraintSet textConstraints = new ConstraintSet();
	private final ConstraintSet imageConstraints = new ConstraintSet();
	private final ConstraintSet imageTextConstraints = new ConstraintSet();
	private final LinearLayout audioLayout;
	private ConversationListener listener;
	ConversationMessageViewHolder(View v, ConversationListener listener,
			boolean isIncoming, RecycledViewPool imageViewPool,
			ImageItemDecoration imageItemDecoration) {
		super(v, listener, isIncoming);
		this.listener=listener;
		statusLayout = v.findViewById(R.id.statusLayout);

		// image list
		RecyclerView list = v.findViewById(R.id.imageList);
		list.setRecycledViewPool(imageViewPool);

		text=v.findViewById(R.id.text);

		audioLayout = (LinearLayout) v.findViewById(R.id.audioLayout);
		adapter = new ImageAdapter(v.getContext(), listener);
		list.setAdapter(adapter);

		list.addItemDecoration(imageItemDecoration);

		// remember original status text color
		timeColor = time.getCurrentTextColor();
		timeColorBubble =
				getColor(v.getContext(), R.color.msg_status_bubble_foreground);

		// clone constraint sets from layout files
		textConstraints.clone(v.getContext(),
				R.layout.list_item_conversation_msg_in_content);
		imageConstraints.clone(v.getContext(),
				R.layout.list_item_conversation_msg_image);
		imageTextConstraints.clone(v.getContext(),
				R.layout.list_item_conversation_msg_image_text);

		// in/out are different layouts, so we need to do this only once
		textConstraints
				.setHorizontalBias(R.id.statusLayout, isIncoming() ? 1 : 0);
		imageConstraints
				.setHorizontalBias(R.id.statusLayout, isIncoming() ? 1 : 0);
		imageTextConstraints
				.setHorizontalBias(R.id.statusLayout, isIncoming() ? 1 : 0);
	}

	@Override
	void bind(ConversationItem conversationItem, boolean selected) {
		super.bind(conversationItem, selected);
		ConversationMessageItem item =
				(ConversationMessageItem) conversationItem;
		if (item.getAttachments().isEmpty()) {
			bindTextItem();
		} else {
			if(item.getAttachments().size()==1 &&
			item.getAttachments().get(0).getHeader().
					getContentType().startsWith("audio/")){
				bindAudioItem(item);
			}else{
				bindImageItem(item);
			}

		}
	}

	private void bindTextItem() {
		resetStatusLayoutForText();
		textConstraints.applyTo(layout);
		adapter.clear();
	}

	private void bindImageItem(ConversationMessageItem item) {
		ConstraintSet constraintSet;
		if (item.getText() == null) {
			statusLayout.setBackgroundResource(R.drawable.msg_status_bubble);
			time.setTextColor(timeColorBubble);
			setImageTintList(bomb, ColorStateList.valueOf(timeColorBubble));
			constraintSet = imageConstraints;
		} else {
			resetStatusLayoutForText();
			constraintSet = imageTextConstraints;
		}

		if (item.getAttachments().size() == 1) {
			// apply image size constraints for a single image
			AttachmentItem attachment = item.getAttachments().get(0);
			int width = attachment.getThumbnailWidth();
			int height = attachment.getThumbnailHeight();
			constraintSet.constrainWidth(R.id.imageList, width);
			constraintSet.constrainHeight(R.id.imageList, height);
		} else {
			// bubble adapts to size of image list
			constraintSet.constrainWidth(R.id.imageList, WRAP_CONTENT);
			constraintSet.constrainHeight(R.id.imageList, WRAP_CONTENT);
		}
		constraintSet.applyTo(layout);
		adapter.setConversationItem(item);
	}



	private void bindAudioItem(ConversationMessageItem item) {
		audioLayout.setVisibility(View.VISIBLE);
		TextView txtDuration=audioLayout.findViewById(R.id.txtDuration);
		ImageView imgPlay=audioLayout.findViewById(R.id.audioImage);

		String durationText="";
		try{
			int durationVal=Integer.parseInt(item.getText());
			int hour=durationVal/60;
			int minute=(durationVal-hour*60);
			String sHour=Integer.toString(hour);
			if(hour<10){
				sHour="0"+sHour;
			}
			String sMinute=Integer.toString(minute);
			if(minute<10){
				sMinute="0"+sMinute;
			}
			durationText=sHour+":"+sMinute;

		}
		catch(Exception e){}
		txtDuration.setText(durationText);
		text.setVisibility(View.GONE);
		audioLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(playing){
					playing=false;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						imgPlay.setImageDrawable(itemView.getContext()
								.getDrawable(
										android.R.drawable.ic_media_play));
					}
					listener.onSpeechStopped();
				}else {
					playing=true;
					//AttachmentItem aItem=new AttachmentItem();
					;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

						imgPlay.setImageDrawable(itemView.getContext()
								.getDrawable(
										android.R.drawable.ic_media_pause));
					}

					listener.onSpeechAttachmentClicked(
							ConversationMessageViewHolder.this, item,
							item.getAttachments().get(0), true);
					//item.getAttachments();
				}



			}
		});
	}

	public void onSpeechStop(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			ImageView imgPlay=audioLayout.findViewById(R.id.audioImage);
			imgPlay.setImageDrawable(itemView.getContext().getDrawable(android.R.drawable.ic_media_play));
		}
	}

	private void resetStatusLayoutForText() {
		statusLayout.setBackgroundResource(0);
		// also reset padding (the background drawable defines some)
		statusLayout.setPadding(0, 0, 0, 0);
		time.setTextColor(timeColor);
		setImageTintList(bomb, ColorStateList.valueOf(timeColor));
	}

}

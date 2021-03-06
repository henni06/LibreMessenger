package org.libreproject.libre.android.account;


import android.content.Context;
import android.util.AttributeSet;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.R;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import static org.libreproject.libre.android.util.UiUtils.needsDozeWhitelisting;

@UiThread
@NotNullByDefault
class DozeView extends PowerView {

	@Nullable
	private Runnable onButtonClickListener;

	public DozeView(Context context) {
		this(context, null);
	}

	public DozeView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DozeView(Context context, @Nullable AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setText(R.string.setup_doze_intro);
		setButtonText(R.string.setup_doze_button);
	}

	@Override
	public boolean needsToBeShown() {
		return needsToBeShown(getContext());
	}

	public static boolean needsToBeShown(Context context) {
		return needsDozeWhitelisting(context);
	}

	@Override
	protected int getHelpText() {
		return R.string.setup_doze_explanation;
	}

	@Override
	protected void onButtonClick() {
		if (onButtonClickListener == null) throw new IllegalStateException();
		onButtonClickListener.run();
	}

	public void setOnButtonClickListener(Runnable runnable) {
		onButtonClickListener = runnable;
	}

}

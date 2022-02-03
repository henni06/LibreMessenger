package org.libreproject.libre.android.account;


import android.content.Context;
import android.util.AttributeSet;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.R;

import javax.annotation.Nullable;

import androidx.annotation.StringRes;
import androidx.annotation.UiThread;

import static android.os.Build.BRAND;
import static org.libreproject.bramble.util.AndroidUtils.getSystemProperty;
import static org.libreproject.bramble.util.StringUtils.isNullOrEmpty;
import static org.libreproject.libre.android.util.UiUtils.showOnboardingDialog;

@UiThread
@NotNullByDefault
class XiaomiView extends PowerView {

	public XiaomiView(Context context) {
		this(context, null);
	}

	public XiaomiView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public XiaomiView(Context context, @Nullable AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setText(R.string.setup_xiaomi_text);
		setButtonText(R.string.setup_xiaomi_button);
	}

	@Override
	public boolean needsToBeShown() {
		return isXiaomiOrRedmiDevice();
	}

	public static boolean isXiaomiOrRedmiDevice() {
		return "Xiaomi".equalsIgnoreCase(BRAND) ||
				"Redmi".equalsIgnoreCase(BRAND);
	}

	@Override
	@StringRes
	protected int getHelpText() {
		return R.string.setup_xiaomi_help;
	}

	@Override
	protected void onButtonClick() {
		int bodyRes = isMiuiTenOrLater()
				? R.string.setup_xiaomi_dialog_body_new
				: R.string.setup_xiaomi_dialog_body_old;
		showOnboardingDialog(getContext(), getContext().getString(bodyRes));
		setChecked(true);
	}

	private boolean isMiuiTenOrLater() {
		String version = getSystemProperty("ro.miui.ui.version.name");
		if (isNullOrEmpty(version)) return false;
		version = version.replaceAll("[^\\d]", "");
		try {
			return Integer.parseInt(version) >= 10;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}

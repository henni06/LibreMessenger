package org.libreproject.libre.android.logout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.libre.R;
import org.libreproject.libre.android.fragment.BaseFragment;

import javax.annotation.Nullable;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class SignOutFragment extends BaseFragment {

	public static final String TAG = SignOutFragment.class.getName();

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_sign_out, container, false);
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}
}

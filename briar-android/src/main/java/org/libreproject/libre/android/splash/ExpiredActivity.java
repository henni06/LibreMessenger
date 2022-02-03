package org.libreproject.libre.android.splash;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import org.libreproject.libre.R;
import org.libreproject.libre.android.Localizer;

import androidx.appcompat.app.AppCompatActivity;

import static android.content.Intent.ACTION_VIEW;
import static android.view.WindowManager.LayoutParams.FLAG_SECURE;
import static org.libreproject.libre.android.TestingConstants.PREVENT_SCREENSHOTS;

public class ExpiredActivity extends AppCompatActivity
		implements OnClickListener {

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);

		if (PREVENT_SCREENSHOTS) getWindow().addFlags(FLAG_SECURE);

		setContentView(R.layout.activity_expired);
		findViewById(R.id.download_briar_button).setOnClickListener(this);
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(
				Localizer.getInstance().setLocale(base));
		Localizer.getInstance().setLocale(this);
	}

	@Override
	public void onClick(View v) {
		Uri uri = Uri.parse("https://briarproject.org/download.html");
		startActivity(new Intent(ACTION_VIEW, uri));
		finish();
	}
}

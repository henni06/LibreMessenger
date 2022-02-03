package org.libreproject.libre.android.qrcode;

import android.hardware.Camera;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import androidx.annotation.UiThread;

@NotNullByDefault
public interface PreviewConsumer {

	@UiThread
	void start(Camera camera, int cameraIndex);

	@UiThread
	void stop();
}

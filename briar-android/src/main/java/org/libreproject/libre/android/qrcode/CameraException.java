package org.libreproject.libre.android.qrcode;

import java.io.IOException;

public class CameraException extends IOException {

	CameraException(String message) {
		super(message);
	}

	CameraException(Throwable cause) {
		super(cause);
	}
}

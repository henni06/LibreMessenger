package org.briarproject.briar.android.keyagreement;

import android.graphics.Bitmap;
import android.util.DisplayMetrics;

import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;

import java.util.logging.Logger;

import javax.annotation.Nullable;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static com.google.zxing.BarcodeFormat.QR_CODE;
import static java.util.logging.Level.WARNING;
import static org.briarproject.bramble.util.LogUtils.logException;

@NotNullByDefault
class QrCodeUtils {

	private static final Logger LOG =
			Logger.getLogger(QrCodeUtils.class.getName());

	@Nullable
	static Bitmap createQrCode(DisplayMetrics dm, String input) {
		int smallestDimen = Math.min(dm.widthPixels, dm.heightPixels);
		try {
			// Generate QR code
			BitMatrix encoded = new QRCodeWriter().encode(input, QR_CODE,
					smallestDimen, smallestDimen);
			return renderQrCode(encoded);
		} catch (WriterException e) {
			logException(LOG, WARNING, e);
			return null;
		}
	}

	private static Bitmap renderQrCode(BitMatrix matrix) {
		int width = matrix.getWidth();
		int height = matrix.getHeight();
		int[] pixels = new int[width * height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				pixels[y * width + x] = matrix.get(x, y) ? BLACK : WHITE;
			}
		}
		Bitmap qr = Bitmap.createBitmap(width, height, ARGB_8888);
		qr.setPixels(pixels, 0, width, 0, 0, width, height);
		return qr;
	}
}

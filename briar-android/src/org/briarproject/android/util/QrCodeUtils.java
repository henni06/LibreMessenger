package org.briarproject.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;

import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static com.google.zxing.BarcodeFormat.QR_CODE;
import static java.util.logging.Level.WARNING;

public class QrCodeUtils {

	private static final Logger LOG =
			Logger.getLogger(QrCodeUtils.class.getName());

	@Nullable
	public static Bitmap createQrCode(Context context, String input) {
		// Get narrowest screen dimension
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		int smallestDimen = Math.min(dm.widthPixels, dm.heightPixels);
		try {
			// Generate QR code
			final BitMatrix encoded = new QRCodeWriter().encode(
					input, QR_CODE, smallestDimen, smallestDimen);
			// Convert QR code to Bitmap
			int width = encoded.getWidth();
			int height = encoded.getHeight();
			int[] pixels = new int[width * height];
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					pixels[y * width + x] = encoded.get(x, y) ? BLACK : WHITE;
				}
			}
			Bitmap qr = Bitmap.createBitmap(width, height, ARGB_8888);
			qr.setPixels(pixels, 0, width, 0, 0, width, height);
			return qr;
		} catch (WriterException e) {
			if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
			return null;
		}
	}
}

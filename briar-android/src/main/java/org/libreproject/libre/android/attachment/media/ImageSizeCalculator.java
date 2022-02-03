package org.libreproject.libre.android.attachment.media;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.InputStream;

@NotNullByDefault
public interface ImageSizeCalculator {

	/**
	 * Determine the size of the image that can be read from {@code is}.
	 *
	 * @param contentType the mime type of the image. If "image/jpeg" is passed,
	 * the implementation will try to determine the size from the Exif header
	 */
	Size getSize(InputStream is, String contentType);

}

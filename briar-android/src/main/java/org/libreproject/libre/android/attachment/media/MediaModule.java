package org.libreproject.libre.android.attachment.media;

import dagger.Module;
import dagger.Provides;

@Module
public class MediaModule {

	@Provides
	ImageHelper provideImageHelper(ImageHelperImpl imageHelper) {
		return imageHelper;
	}

	@Provides
	ImageSizeCalculator provideImageSizeCalculator(ImageHelper imageHelper) {
		return new ImageSizeCalculatorImpl(imageHelper);
	}

	@Provides
	ImageCompressor provideImageCompressor(
			ImageCompressorImpl imageCompressor) {
		return imageCompressor;
	}
}

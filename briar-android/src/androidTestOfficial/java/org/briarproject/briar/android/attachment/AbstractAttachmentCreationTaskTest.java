package org.briarproject.briar.android.attachment;

import android.content.res.AssetManager;

import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;

import static androidx.test.InstrumentationRegistry.getContext;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

abstract class AbstractAttachmentCreationTaskTest {

	private final ImageHelper imageHelper = new ImageHelperImpl();
	private final ImageSizeCalculator imageSizeCalculator =
			new ImageSizeCalculator(imageHelper);

	private AttachmentCreationTask task;

	@Before
	@SuppressWarnings("ConstantConditions")  // add real objects when needed
	public void setUp() {
		task = new AttachmentCreationTask(null,
				getApplicationContext().getContentResolver(), null,
				imageSizeCalculator, null, null, true);
	}

	void testCompress(String filename, String contentType)
			throws IOException {
		InputStream is = getAssetManager().open(filename);
		task.compressImage(is, contentType);
	}

	static AssetManager getAssetManager() {
		// pm.getResourcesForApplication(packageName).getAssets() did not work
		//noinspection deprecation
		return getContext().getAssets();
	}

}

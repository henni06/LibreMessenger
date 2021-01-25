package org.briarproject.briar.android.attachment.media;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.logging.Logger.getLogger;
import static org.briarproject.bramble.api.nullsafety.NullSafety.requireNonNull;
import static org.briarproject.bramble.test.TestUtils.isOptionalTestEnabled;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public class PngSuiteImageCompressorTest
		extends AbstractImageCompressorTest {

	private static final Logger LOG =
			getLogger(PngSuiteImageCompressorTest.class.getName());

	@Override
	protected void inject(AbstractImageCompressorComponent component) {
		component.inject(this);
	}

	@Parameters
	public static Iterable<String> data() throws IOException {
		List<String> data = new ArrayList<>();
		String[] files = requireNonNull(getAssetManager().list("PngSuite"));
		for (String filename : files)
			if (filename.endsWith(".png")) data.add(filename);
		return data;
	}

	private final String filename;

	public PngSuiteImageCompressorTest(String filename) {
		this.filename = filename;
	}

	@Test
	public void testPngSuiteCompress() throws Exception {
		assumeTrue(isOptionalTestEnabled(
				PngSuiteImageCompressorTest.class));
		LOG.info("Testing " + filename);
		if (filename.startsWith("x")) {
			try {
				testCompress("PngSuite/" + filename, "image/png");
				fail();
			} catch (IOException expected) {
				// Expected
			}
		} else {
			testCompress("PngSuite/" + filename, "image/png");
		}
	}
}

package org.libreproject.libre.api.test;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

public interface TestAvatarCreator {
	@Nullable
	InputStream getAvatarInputStream() throws IOException;
}

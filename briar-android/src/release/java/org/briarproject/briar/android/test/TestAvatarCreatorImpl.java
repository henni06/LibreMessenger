package org.libreproject.libre.android.test;

import org.libreproject.libre.api.test.TestAvatarCreator;

import java.io.InputStream;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class TestAvatarCreatorImpl implements TestAvatarCreator {

	@Inject
	TestAvatarCreatorImpl() {
	}

	@Nullable
	@Override
	public InputStream getAvatarInputStream() {
		return null;
	}
}

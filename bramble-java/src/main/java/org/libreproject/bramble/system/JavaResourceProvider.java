package org.libreproject.bramble.system;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.system.ResourceProvider;

import java.io.InputStream;

import javax.inject.Inject;

import static org.libreproject.bramble.api.nullsafety.NullSafety.requireNonNull;

@NotNullByDefault
class JavaResourceProvider implements ResourceProvider {

	@Inject
	JavaResourceProvider() {
	}

	@Override
	public InputStream getResourceInputStream(String name, String extension) {
		ClassLoader cl = getClass().getClassLoader();
		return requireNonNull(cl.getResourceAsStream(name + extension));
	}
}

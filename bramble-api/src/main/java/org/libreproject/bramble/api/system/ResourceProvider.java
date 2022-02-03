package org.libreproject.bramble.api.system;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.InputStream;

@NotNullByDefault
public interface ResourceProvider {

	InputStream getResourceInputStream(String name, String extension);
}

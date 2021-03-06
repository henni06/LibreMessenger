package org.libreproject.bramble.api.plugin.file;

import org.libreproject.bramble.api.plugin.TransportId;

public interface RemovableDriveConstants {

	TransportId ID = new TransportId("org.briarproject.bramble.drive");

	String PROP_PATH = "path";
	String PROP_URI = "uri";
	String PROP_SUPPORTED = "supported";
}

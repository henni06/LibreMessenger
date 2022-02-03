package org.libreproject.bramble.api.reporting;

import org.libreproject.bramble.api.crypto.PublicKey;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.File;

@NotNullByDefault
public interface DevConfig {

	PublicKey getDevPublicKey();

	String getDevOnionAddress();

	File getReportDir();

	File getLogcatFile();
}

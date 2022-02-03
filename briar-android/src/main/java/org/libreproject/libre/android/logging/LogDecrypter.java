package org.libreproject.libre.android.logging;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.util.AndroidUtils;

import androidx.annotation.Nullable;

@NotNullByDefault
public interface LogDecrypter {
	/**
	 * Returns decrypted log records from {@link AndroidUtils#getLogcatFile}
	 * or null if there was an error reading the logs.
	 */
	@Nullable
	String decryptLogs(@Nullable byte[] logKey);
}

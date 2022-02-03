package org.libreproject.libre.android.logging;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.util.AndroidUtils;

import androidx.annotation.Nullable;

@NotNullByDefault
public interface LogEncrypter {
	/**
	 * Writes encrypted log records to {@link AndroidUtils#getLogcatFile}
	 * and returns the encryption key if everything went fine.
	 */
	@Nullable
	byte[] encryptLogs();
}

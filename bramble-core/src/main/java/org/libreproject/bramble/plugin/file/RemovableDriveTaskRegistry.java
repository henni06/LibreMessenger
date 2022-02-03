package org.libreproject.bramble.plugin.file;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.file.RemovableDriveTask;

@NotNullByDefault
interface RemovableDriveTaskRegistry {

	void removeReader(RemovableDriveTask task);

	void removeWriter(RemovableDriveTask task);
}

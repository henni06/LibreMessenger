package org.libreproject.bramble.plugin.file;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.file.RemovableDriveTask;
import org.libreproject.bramble.api.properties.TransportProperties;

@NotNullByDefault
interface RemovableDriveTaskFactory {

	RemovableDriveTask createReader(RemovableDriveTaskRegistry registry,
			TransportProperties p);

	RemovableDriveTask createWriter(RemovableDriveTaskRegistry registry,
			ContactId c, TransportProperties p);
}

package org.libreproject.bramble.api.data;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.OutputStream;

@NotNullByDefault
public interface BdfWriterFactory {

	BdfWriter createWriter(OutputStream out);
}

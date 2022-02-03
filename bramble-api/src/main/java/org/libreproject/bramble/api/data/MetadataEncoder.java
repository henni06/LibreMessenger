package org.libreproject.bramble.api.data;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.db.Metadata;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface MetadataEncoder {

	Metadata encode(BdfDictionary d) throws FormatException;
}

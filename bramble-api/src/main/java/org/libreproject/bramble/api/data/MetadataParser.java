package org.libreproject.bramble.api.data;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.db.Metadata;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface MetadataParser {

	BdfDictionary parse(Metadata m) throws FormatException;
}

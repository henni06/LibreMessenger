package org.libreproject.libre.privategroup.invitation;

import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
interface SessionEncoder {

	BdfDictionary encodeSession(Session s);
}

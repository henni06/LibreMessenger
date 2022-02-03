package org.libreproject.libre.introduction;

import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.identity.Author;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
interface SessionEncoder {

	BdfDictionary getIntroduceeSessionsByIntroducerQuery(Author introducer);

	BdfDictionary getIntroducerSessionsQuery();

	BdfDictionary encodeIntroducerSession(IntroducerSession s);

	BdfDictionary encodeIntroduceeSession(IntroduceeSession s);

}

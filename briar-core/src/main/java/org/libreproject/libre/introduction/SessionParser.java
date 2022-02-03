package org.libreproject.libre.introduction;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.api.client.SessionId;
import org.libreproject.libre.api.introduction.Role;

@NotNullByDefault
interface SessionParser {

	BdfDictionary getSessionQuery(SessionId s);

	Role getRole(BdfDictionary d) throws FormatException;

	IntroducerSession parseIntroducerSession(BdfDictionary d)
			throws FormatException;

	IntroduceeSession parseIntroduceeSession(GroupId introducerGroupId,
			BdfDictionary d) throws FormatException;

}

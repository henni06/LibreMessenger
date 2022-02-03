package org.libreproject.libre.privategroup.invitation;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.api.client.SessionId;

@NotNullByDefault
interface SessionParser {

	BdfDictionary getSessionQuery(SessionId s);

	BdfDictionary getAllSessionsQuery();

	Role getRole(BdfDictionary d) throws FormatException;

	boolean isSession(BdfDictionary d);

	Session parseSession(GroupId contactGroupId, BdfDictionary d)
			throws FormatException;

	CreatorSession parseCreatorSession(GroupId contactGroupId, BdfDictionary d)
			throws FormatException;

	InviteeSession parseInviteeSession(GroupId contactGroupId, BdfDictionary d)
			throws FormatException;

	PeerSession parsePeerSession(GroupId contactGroupId, BdfDictionary d)
			throws FormatException;
}

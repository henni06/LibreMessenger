package org.libreproject.libre.privategroup.invitation;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class AbortMessage extends GroupInvitationMessage {

	AbortMessage(MessageId id, GroupId contactGroupId, GroupId privateGroupId,
			long timestamp) {
		super(id, contactGroupId, privateGroupId, timestamp);
	}
}

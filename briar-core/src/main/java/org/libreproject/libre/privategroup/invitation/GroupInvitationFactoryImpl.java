package org.libreproject.libre.privategroup.invitation;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.client.ContactGroupFactory;
import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.crypto.PrivateKey;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.identity.AuthorId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.Group;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationFactory;

import java.security.GeneralSecurityException;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.libreproject.libre.api.privategroup.invitation.GroupInvitationManager.CLIENT_ID;
import static org.libreproject.libre.api.privategroup.invitation.GroupInvitationManager.MAJOR_VERSION;

@Immutable
@NotNullByDefault
class GroupInvitationFactoryImpl implements GroupInvitationFactory {

	private final ContactGroupFactory contactGroupFactory;
	private final ClientHelper clientHelper;

	@Inject
	GroupInvitationFactoryImpl(ContactGroupFactory contactGroupFactory,
			ClientHelper clientHelper) {
		this.contactGroupFactory = contactGroupFactory;
		this.clientHelper = clientHelper;
	}

	@Override
	public byte[] signInvitation(Contact c, GroupId privateGroupId,
			long timestamp, PrivateKey privateKey) {
		AuthorId creatorId = c.getLocalAuthorId();
		AuthorId memberId = c.getAuthor().getId();
		BdfList token = createInviteToken(creatorId, memberId, privateGroupId,
				timestamp);
		try {
			return clientHelper.sign(SIGNING_LABEL_INVITE, token, privateKey);
		} catch (GeneralSecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (FormatException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public BdfList createInviteToken(AuthorId creatorId, AuthorId memberId,
			GroupId privateGroupId, long timestamp) {
		Group contactGroup = contactGroupFactory.createContactGroup(CLIENT_ID,
				MAJOR_VERSION, creatorId, memberId);
		return BdfList.of(
				timestamp,
				contactGroup.getId(),
				privateGroupId
		);
	}

}

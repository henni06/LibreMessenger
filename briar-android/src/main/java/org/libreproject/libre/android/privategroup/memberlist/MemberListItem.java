package org.libreproject.libre.android.privategroup.memberlist;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.identity.Author;
import org.libreproject.libre.api.identity.AuthorInfo;
import org.libreproject.libre.api.identity.AuthorInfo.Status;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.privategroup.GroupMember;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
@NotNullByDefault
class MemberListItem {

	private final GroupMember groupMember;
	private boolean online;

	MemberListItem(GroupMember groupMember, boolean online) {
		this.groupMember = groupMember;
		this.online = online;
	}

	Author getMember() {
		return groupMember.getAuthor();
	}

	AuthorInfo getAuthorInfo() {
		return groupMember.getAuthorInfo();
	}

	Status getStatus() {
		return groupMember.getAuthorInfo().getStatus();
	}

	boolean isCreator() {
		return groupMember.isCreator();
	}

	@Nullable
	ContactId getContactId() {
		return groupMember.getContactId();
	}

	boolean isOnline() {
		return online;
	}

	void setOnline(boolean online) {
		this.online = online;
	}

}

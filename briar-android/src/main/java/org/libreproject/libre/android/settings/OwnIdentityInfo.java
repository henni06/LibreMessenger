package org.libreproject.libre.android.settings;

import org.libreproject.bramble.api.identity.LocalAuthor;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.api.identity.AuthorInfo;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class OwnIdentityInfo {

	private final LocalAuthor localAuthor;
	private final AuthorInfo authorInfo;

	OwnIdentityInfo(LocalAuthor localAuthor, AuthorInfo authorInfo) {
		this.localAuthor = localAuthor;
		this.authorInfo = authorInfo;
	}

	LocalAuthor getLocalAuthor() {
		return localAuthor;
	}

	AuthorInfo getAuthorInfo() {
		return authorInfo;
	}

}
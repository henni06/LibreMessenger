package org.libreproject.bramble.api.mailbox;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class MailboxProperties {

	private final String onionAddress, authToken;
	private final boolean owner;

	public MailboxProperties(String onionAddress, String authToken,
			boolean owner) {
		this.onionAddress = onionAddress;
		this.authToken = authToken;
		this.owner = owner;
	}

	public String getOnionAddress() {
		return onionAddress;
	}

	public String getAuthToken() {
		return authToken;
	}

	public boolean isOwner() {
		return owner;
	}
}

package org.libreproject.bramble.test;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.system.SecureRandomProvider;

import java.security.Provider;

@NotNullByDefault
public class TestSecureRandomProvider implements SecureRandomProvider {

	@Override
	public Provider getProvider() {
		// Use the default provider
		return null;
	}
}

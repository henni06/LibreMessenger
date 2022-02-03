package org.libreproject.bramble.api.network;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface NetworkManager {

	NetworkStatus getNetworkStatus();
}

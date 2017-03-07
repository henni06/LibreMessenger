package org.briarproject.bramble.api.plugin;

public interface TorConstants {

	TransportId ID = new TransportId("org.briarproject.bramble.tor");

	int SOCKS_PORT = 59050;
	int CONTROL_PORT = 59051;

	int CONNECT_TO_PROXY_TIMEOUT = 5000; // Milliseconds

	String PREF_TOR_NETWORK = "network";
	String PREF_TOR_PORT = "port";
}

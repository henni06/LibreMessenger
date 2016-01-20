package org.briarproject.plugins.tcp;

import org.briarproject.api.Settings;
import org.briarproject.api.TransportProperties;
import org.briarproject.api.contact.ContactId;
import org.briarproject.plugins.DuplexClientTest;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// This is not a JUnit test - it has to be run manually while the server test
// is running on another machine
public class LanTcpClientTest extends DuplexClientTest {

	private static final int MAX_LATENCY = 60 * 1000;
	private static final int MAX_IDLE_TIME = 30 * 1000;
	private static final int POLLING_INTERVAL = 60 * 1000;

	private LanTcpClientTest(Executor executor, String serverAddress,
			String serverPort) {
		// Store the server's internal address and port
		TransportProperties p = new TransportProperties();
		p.put("address", serverAddress);
		p.put("port", serverPort);
		Map<ContactId, TransportProperties> remote =
				Collections.singletonMap(contactId, p);
		// Create the plugin
		callback = new ClientCallback(new Settings(),
				new TransportProperties(), remote);
		plugin = new LanTcpPlugin(executor, callback,  MAX_LATENCY,
				MAX_IDLE_TIME, POLLING_INTERVAL);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Please specify the server's address and port");
			System.exit(1);
		}
		ExecutorService executor = Executors.newCachedThreadPool();
		try {
			new LanTcpClientTest(executor, args[0], args[1]).run();
		} finally {
			executor.shutdown();
		}
	}
}

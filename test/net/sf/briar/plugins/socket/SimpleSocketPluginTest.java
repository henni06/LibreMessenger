package net.sf.briar.plugins.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;
import net.sf.briar.api.ContactId;
import net.sf.briar.api.transport.stream.StreamTransportCallback;
import net.sf.briar.api.transport.stream.StreamTransportConnection;

import org.junit.Before;
import org.junit.Test;

public class SimpleSocketPluginTest extends TestCase {

	private final ContactId contactId = new ContactId(0);

	private Map<String, String> localProperties = null;
	private Map<ContactId, Map<String, String>> remoteProperties = null;
	private Map<String, String> config = null;

	@Before
	public void setUp() {
		localProperties = new TreeMap<String, String>();
		remoteProperties = new HashMap<ContactId, Map<String, String>>();
		remoteProperties.put(contactId, new TreeMap<String, String>());
		config = new TreeMap<String, String>();
	}

	@Test
	public void testIncomingConnection() throws Exception {
		StubCallback callback = new StubCallback();
		localProperties.put("host", "127.0.0.1");
		localProperties.put("port", "0");
		SimpleSocketPlugin plugin =
			new SimpleSocketPlugin(new ImmediateExecutor(), 10);
		plugin.start(localProperties, remoteProperties, config, callback);
		// The plugin should have bound a socket and stored the port number
		assertNotNull(callback.localProperties);
		String host = callback.localProperties.get("host");
		assertNotNull(host);
		assertEquals("127.0.0.1", host);
		String portString = callback.localProperties.get("port");
		assertNotNull(portString);
		int port = Integer.valueOf(portString);
		assertTrue(port > 0 && port < 65536);
		// The plugin should be listening on the port
		InetSocketAddress addr = new InetSocketAddress(host, port);
		Socket s = new Socket();
		assertEquals(0, callback.incomingConnections);
		s.connect(addr, 100);
		Thread.sleep(100);
		assertEquals(1, callback.incomingConnections);
		s.close();
		// Stop the plugin
		plugin.stop();
		// The plugin should no longer be listening
		try {
			s.connect(addr, 100);
			fail();
		} catch(IOException expected) {}
	}

	@Test
	public void testOutgoingConnection() throws Exception {
		StubCallback callback = new StubCallback();
		SimpleSocketPlugin plugin =
			new SimpleSocketPlugin(new ImmediateExecutor(), 10);
		plugin.start(localProperties, remoteProperties, config, callback);
		// Listen on a local port
		final ServerSocket ss = new ServerSocket();
		ss.bind(new InetSocketAddress("127.0.0.1", 0), 10);
		int port = ss.getLocalPort();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean error = new AtomicBoolean(false);
		new Thread() {
			@Override
			public void run() {
				try {
					ss.accept();
					latch.countDown();
				} catch(IOException e) {
					error.set(true);
				}
			}
		}.start();
		// Tell the plugin about the port
		Map<String, String> properties = new TreeMap<String, String>();
		properties.put("host", "127.0.0.1");
		properties.put("port", String.valueOf(port));
		plugin.setRemoteProperties(contactId, properties);
		// Connect to the port
		StreamTransportConnection conn = plugin.createConnection(contactId);
		assertNotNull(conn);
		// Check that the connection was accepted
		assertTrue(latch.await(1, TimeUnit.SECONDS));
		assertFalse(error.get());
		// Clean up
		conn.getInputStream().close(); // FIXME: Change the API
		ss.close();
		plugin.stop();
	}

	private static class ImmediateExecutor implements Executor {

		public void execute(Runnable r) {
			r.run();
		}
	}

	private static class StubCallback implements StreamTransportCallback {

		private Map<String, String> localProperties = null;
		private volatile int incomingConnections = 0;

		public void setLocalProperties(Map<String, String> properties) {
			localProperties = properties;
		}

		public void setConfig(Map<String, String> config) {
		}

		public void showMessage(String... message) {
		}

		public boolean showConfirmationMessage(String... message) {
			return false;
		}

		public int showChoice(String[] choices, String... message) {
			return -1;
		}

		public void incomingConnectionCreated(StreamTransportConnection c) {
			incomingConnections++;
		}

		public void outgoingConnectionCreated(ContactId contactId,
				StreamTransportConnection c) {
		}
	}
}

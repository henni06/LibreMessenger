package org.briarproject.plugins.tcp;

import org.briarproject.api.TransportId;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.plugins.Backoff;
import org.briarproject.api.plugins.duplex.DuplexPluginCallback;
import org.briarproject.api.properties.TransportProperties;
import org.briarproject.api.settings.Settings;
import org.briarproject.util.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

class LanTcpPlugin extends TcpPlugin {

	static final TransportId ID = new TransportId("lan");

	private static final int MAX_ADDRESSES = 5;
	private static final String PROP_IP_PORTS = "ipPorts";
	private static final String SEPARATOR = ",";

	LanTcpPlugin(Executor ioExecutor, Backoff backoff,
			DuplexPluginCallback callback, int maxLatency, int maxIdleTime) {
		super(ioExecutor, backoff, callback, maxLatency, maxIdleTime);
	}

	@Override
	public TransportId getId() {
		return ID;
	}

	@Override
	protected List<SocketAddress> getLocalSocketAddresses() {
		// Use the same address and port as last time if available
		TransportProperties p = callback.getLocalProperties();
		String oldIpPorts = p.get(PROP_IP_PORTS);
		List<InetSocketAddress> olds = parseSocketAddresses(oldIpPorts);
		List<SocketAddress> locals = new LinkedList<SocketAddress>();
		for (InetAddress local : getLocalIpAddresses()) {
			if (isAcceptableAddress(local)) {
				// If this is the old address, try to use the same port
				for (InetSocketAddress old : olds) {
					if (old.getAddress().equals(local)) {
						int port = old.getPort();
						locals.add(0, new InetSocketAddress(local, port));
					}
				}
				locals.add(new InetSocketAddress(local, 0));
			}
		}
		return locals;
	}

	private List<InetSocketAddress> parseSocketAddresses(String ipPorts) {
		if (StringUtils.isNullOrEmpty(ipPorts)) return Collections.emptyList();
		String[] split = ipPorts.split(SEPARATOR);
		List<InetSocketAddress> remotes = new ArrayList<InetSocketAddress>();
		for (String ipPort : split) {
			InetSocketAddress a = parseSocketAddress(ipPort);
			if (a != null) remotes.add(a);
		}
		return remotes;
	}

	@Override
	protected void setLocalSocketAddress(InetSocketAddress a) {
		String ipPort = getIpPortString(a);
		// Get the list of recently used addresses
		String setting = callback.getSettings().get(PROP_IP_PORTS);
		List<String> recent = new ArrayList<String>();
		if (!StringUtils.isNullOrEmpty(setting))
			Collections.addAll(recent, setting.split(SEPARATOR));
		// Is the address already in the list?
		if (recent.remove(ipPort)) {
			// Move the address to the start of the list
			recent.add(0, ipPort);
			setting = StringUtils.join(recent, SEPARATOR);
		} else {
			// Add the address to the start of the list
			recent.add(0, ipPort);
			// Drop the least recently used address if the list is full
			if (recent.size() > MAX_ADDRESSES)
				recent = recent.subList(0, MAX_ADDRESSES);
			setting = StringUtils.join(recent, SEPARATOR);
			// Update the list of addresses shared with contacts
			List<String> shared = new ArrayList<String>(recent);
			Collections.sort(shared);
			String property = StringUtils.join(shared, SEPARATOR);
			TransportProperties properties = new TransportProperties();
			properties.put(PROP_IP_PORTS, property);
			callback.mergeLocalProperties(properties);
		}
		// Save the setting
		Settings settings = new Settings();
		settings.put(PROP_IP_PORTS, setting);
		callback.mergeSettings(settings);
	}

	@Override
	protected List<InetSocketAddress> getRemoteSocketAddresses(ContactId c) {
		TransportProperties p = callback.getRemoteProperties().get(c);
		if (p == null) return Collections.emptyList();
		return parseSocketAddresses(p.get(PROP_IP_PORTS));
	}

	private boolean isAcceptableAddress(InetAddress a) {
		// Accept link-local and site-local IPv4 addresses
		boolean ipv4 = a instanceof Inet4Address;
		boolean loop = a.isLoopbackAddress();
		boolean link = a.isLinkLocalAddress();
		boolean site = a.isSiteLocalAddress();
		return ipv4 && !loop && (link || site);
	}

	@Override
	protected boolean isConnectable(InetSocketAddress remote) {
		if (remote.getPort() == 0) return false;
		if (!isAcceptableAddress(remote.getAddress())) return false;
		// Try to determine whether the address is on the same LAN as us
		if (socket == null) return false;
		byte[] localIp = socket.getInetAddress().getAddress();
		byte[] remoteIp = remote.getAddress().getAddress();
		return addressesAreOnSameLan(localIp, remoteIp);
	}

	// Package access for testing
	boolean addressesAreOnSameLan(byte[] localIp, byte[] remoteIp) {
		// 10.0.0.0/8
		if (localIp[0] == 10) return remoteIp[0] == 10;
		// 172.16.0.0/12
		if (localIp[0] == (byte) 172 && (localIp[1] & 0xF0) == 16)
			return remoteIp[0] == (byte) 172 && (remoteIp[1] & 0xF0) == 16;
		// 192.168.0.0/16
		if (localIp[0] == (byte) 192 && localIp[1] == (byte) 168)
			return remoteIp[0] == (byte) 192 && remoteIp[1] == (byte) 168;
		// Unrecognised prefix - may be compatible
		return true;
	}
}

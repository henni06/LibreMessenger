package net.sf.briar.plugins.tcp;

import static android.content.Context.WIFI_SERVICE;

import java.util.concurrent.Executor;

import net.sf.briar.api.clock.Clock;
import net.sf.briar.api.crypto.PseudoRandom;
import net.sf.briar.api.plugins.duplex.DuplexPluginCallback;
import net.sf.briar.api.plugins.duplex.DuplexTransportConnection;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;

class DroidLanTcpPlugin extends LanTcpPlugin {

	private final Context appContext;

	DroidLanTcpPlugin(Executor pluginExecutor, Context appContext, Clock clock,
			DuplexPluginCallback callback, long maxLatency,
			long pollingInterval) {
		super(pluginExecutor, clock, callback, maxLatency, pollingInterval);
		this.appContext = appContext;
	}

	@Override
	public DuplexTransportConnection sendInvitation(PseudoRandom r,
			long timeout) {
		WifiManager wifi =
				(WifiManager) appContext.getSystemService(WIFI_SERVICE);
		if(wifi == null || !wifi.isWifiEnabled()) return null;
		MulticastLock lock = wifi.createMulticastLock("invitation");
		lock.acquire();
		try {
			return super.sendInvitation(r, timeout);
		} finally {
			lock.release();
		}
	}

	@Override
	public DuplexTransportConnection acceptInvitation(PseudoRandom r,
			long timeout) {
		WifiManager wifi =
				(WifiManager) appContext.getSystemService(WIFI_SERVICE);
		if(wifi == null || !wifi.isWifiEnabled()) return null;
		MulticastLock lock = wifi.createMulticastLock("invitation");
		lock.acquire();
		try {
			return super.acceptInvitation(r, timeout);
		} finally {
			lock.release();
		}
	}
}

package org.libreproject.bramble.plugin.tor;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.Plugin;
import org.libreproject.bramble.api.plugin.duplex.AbstractDuplexTransportConnection;
import org.libreproject.bramble.util.IoUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

@NotNullByDefault
class TorTransportConnection extends AbstractDuplexTransportConnection {

	private final Socket socket;

	TorTransportConnection(Plugin plugin, Socket socket) {
		super(plugin);
		this.socket = socket;
	}

	@Override
	protected InputStream getInputStream() throws IOException {
		return IoUtils.getInputStream(socket);
	}

	@Override
	protected OutputStream getOutputStream() throws IOException {
		return IoUtils.getOutputStream(socket);
	}

	@Override
	protected void closeConnection(boolean exception) throws IOException {
		socket.close();
	}
}

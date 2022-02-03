package org.libreproject.bramble.api.sync;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.transport.StreamWriter;

import java.io.InputStream;

import javax.annotation.Nullable;

@NotNullByDefault
public interface SyncSessionFactory {

	SyncSession createIncomingSession(ContactId c, InputStream in,
			PriorityHandler handler);

	SyncSession createSimplexOutgoingSession(ContactId c, TransportId t,
			long maxLatency, boolean eager, StreamWriter streamWriter);

	SyncSession createDuplexOutgoingSession(ContactId c, TransportId t,
			long maxLatency, int maxIdleTime, StreamWriter streamWriter,
			@Nullable Priority priority);
}

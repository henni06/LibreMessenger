package net.sf.briar.protocol;

import java.util.concurrent.Executor;

import net.sf.briar.api.crypto.CryptoComponent;
import net.sf.briar.api.protocol.Ack;
import net.sf.briar.api.protocol.Author;
import net.sf.briar.api.protocol.AuthorFactory;
import net.sf.briar.api.protocol.Group;
import net.sf.briar.api.protocol.GroupFactory;
import net.sf.briar.api.protocol.MessageFactory;
import net.sf.briar.api.protocol.Offer;
import net.sf.briar.api.protocol.PacketFactory;
import net.sf.briar.api.protocol.ProtocolReaderFactory;
import net.sf.briar.api.protocol.ProtocolWriterFactory;
import net.sf.briar.api.protocol.Request;
import net.sf.briar.api.protocol.SubscriptionUpdate;
import net.sf.briar.api.protocol.TransportUpdate;
import net.sf.briar.api.protocol.UnverifiedBatch;
import net.sf.briar.api.protocol.VerificationExecutor;
import net.sf.briar.api.serial.StructReader;
import net.sf.briar.util.BoundedExecutor;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class ProtocolModule extends AbstractModule {

	// FIXME: Determine suitable values for these constants empirically

	/**
	 * The maximum number of verification tasks that can be queued for
	 * execution before submitting another task will block.
	 */
	private static final int MAX_QUEUED_VERIFIER_TASKS = 10;

	/** The minimum number of verification threads to keep in the pool. */
	private static final int MIN_VERIFIER_THREADS = 1;

	/** The maximum number of verification threads. */
	private static final int MAX_VERIFIER_THREADS =
			Runtime.getRuntime().availableProcessors();

	@Override
	protected void configure() {
		bind(AuthorFactory.class).to(AuthorFactoryImpl.class);
		bind(GroupFactory.class).to(GroupFactoryImpl.class);
		bind(MessageFactory.class).to(MessageFactoryImpl.class);
		bind(PacketFactory.class).to(PacketFactoryImpl.class);
		bind(ProtocolReaderFactory.class).to(ProtocolReaderFactoryImpl.class);
		bind(ProtocolWriterFactory.class).to(ProtocolWriterFactoryImpl.class);
		bind(UnverifiedBatchFactory.class).to(UnverifiedBatchFactoryImpl.class);
		// The executor is bounded, so tasks must be independent and short-lived
		bind(Executor.class).annotatedWith(
				VerificationExecutor.class).toInstance(
						new BoundedExecutor(MAX_QUEUED_VERIFIER_TASKS,
								MIN_VERIFIER_THREADS, MAX_VERIFIER_THREADS));
	}

	@Provides
	StructReader<Ack> getAckReader(PacketFactory ackFactory) {
		return new AckReader(ackFactory);
	}

	@Provides
	StructReader<Author> getAuthorReader(CryptoComponent crypto,
			AuthorFactory authorFactory) {
		return new AuthorReader(crypto, authorFactory);
	}

	@Provides
	StructReader<UnverifiedBatch> getBatchReader(
			StructReader<UnverifiedMessage> messageReader,
			UnverifiedBatchFactory batchFactory) {
		return new BatchReader(messageReader, batchFactory);
	}

	@Provides
	StructReader<Group> getGroupReader(CryptoComponent crypto) {
		return new GroupReader(crypto);
	}

	@Provides
	StructReader<UnverifiedMessage> getMessageReader(
			StructReader<Group> groupReader,
			StructReader<Author> authorReader) {
		return new MessageReader(groupReader, authorReader);
	}

	@Provides
	StructReader<Offer> getOfferReader(PacketFactory packetFactory) {
		return new OfferReader(packetFactory);
	}

	@Provides
	StructReader<Request> getRequestReader(PacketFactory packetFactory) {
		return new RequestReader(packetFactory);
	}

	@Provides
	StructReader<SubscriptionUpdate> getSubscriptionReader(
			StructReader<Group> groupReader, PacketFactory packetFactory) {
		return new SubscriptionUpdateReader(groupReader, packetFactory);
	}

	@Provides
	StructReader<TransportUpdate> getTransportReader(
			PacketFactory packetFactory) {
		return new TransportUpdateReader(packetFactory);
	}
}

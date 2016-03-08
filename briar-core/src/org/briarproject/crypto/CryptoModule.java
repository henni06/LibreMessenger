package org.briarproject.crypto;

import org.briarproject.api.crypto.CryptoComponent;
import org.briarproject.api.crypto.CryptoExecutor;
import org.briarproject.api.crypto.PasswordStrengthEstimator;
import org.briarproject.api.crypto.StreamDecrypterFactory;
import org.briarproject.api.crypto.StreamEncrypterFactory;
import org.briarproject.api.lifecycle.LifecycleManager;
import org.briarproject.api.system.SeedProvider;
import org.briarproject.lifecycle.LifecycleModule;

import java.security.SecureRandom;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static java.util.concurrent.TimeUnit.SECONDS;

@Module
public class CryptoModule {

	/**
	 * The maximum number of executor threads.
	 */
	private static final int MAX_EXECUTOR_THREADS =
			Runtime.getRuntime().availableProcessors();

	private final ExecutorService cryptoExecutor;

	public CryptoModule() {
		// Use an unbounded queue
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		// Discard tasks that are submitted during shutdown
		RejectedExecutionHandler policy =
				new ThreadPoolExecutor.DiscardPolicy();
		// Create a limited # of threads and keep them in the pool for 60 secs
		cryptoExecutor = new ThreadPoolExecutor(0, MAX_EXECUTOR_THREADS,
				60, SECONDS, queue, policy);
	}

	@Provides
	AuthenticatedCipher provideAuthenticatedCipher() {
		return new XSalsa20Poly1305AuthenticatedCipher();
	}

	@Provides
	@Singleton
	CryptoComponent provideCryptoComponent(SeedProvider seedProvider) {
		return new CryptoComponentImpl(seedProvider);
	}

	@Provides
	PasswordStrengthEstimator providePasswordStrengthEstimator() {
		return new PasswordStrengthEstimatorImpl();
	}

	@Provides
	StreamDecrypterFactory provideStreamDecrypterFactory(
			Provider<AuthenticatedCipher> cipherProvider) {
		return new StreamDecrypterFactoryImpl(cipherProvider);
	}

	@Provides
	StreamEncrypterFactory provideStreamEncrypterFactory(CryptoComponent crypto,
			Provider<AuthenticatedCipher> cipherProvider) {
		return new StreamEncrypterFactoryImpl(crypto, cipherProvider);
	}

	@Provides
	@Singleton
	@CryptoExecutor
	Executor getCryptoExecutor(LifecycleManager lifecycleManager) {
		lifecycleManager.registerForShutdown(cryptoExecutor);
		return cryptoExecutor;
	}

	@Provides
	SecureRandom getSecureRandom(CryptoComponent crypto) {
		return crypto.getSecureRandom();
	}

}

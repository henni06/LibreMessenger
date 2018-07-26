package org.briarproject.briar.android.login;

import org.briarproject.bramble.api.account.AccountManager;
import org.briarproject.bramble.api.crypto.CryptoComponent;
import org.briarproject.bramble.api.crypto.CryptoExecutor;
import org.briarproject.bramble.api.crypto.PasswordStrengthEstimator;
import org.briarproject.bramble.api.crypto.SecretKey;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.util.StringUtils;
import org.briarproject.briar.android.controller.handler.ResultHandler;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import static org.briarproject.bramble.util.LogUtils.logDuration;
import static org.briarproject.bramble.util.LogUtils.now;

@NotNullByDefault
public class PasswordControllerImpl implements PasswordController {

	private static final Logger LOG =
			Logger.getLogger(PasswordControllerImpl.class.getName());

	protected final AccountManager accountManager;
	protected final Executor cryptoExecutor;
	protected final CryptoComponent crypto;
	private final PasswordStrengthEstimator strengthEstimator;

	@Inject
	PasswordControllerImpl(AccountManager accountManager,
			@CryptoExecutor Executor cryptoExecutor, CryptoComponent crypto,
			PasswordStrengthEstimator strengthEstimator) {
		this.accountManager = accountManager;
		this.cryptoExecutor = cryptoExecutor;
		this.crypto = crypto;
		this.strengthEstimator = strengthEstimator;
	}

	@Override
	public float estimatePasswordStrength(String password) {
		return strengthEstimator.estimateStrength(password);
	}

	@Override
	public void validatePassword(String password,
			ResultHandler<Boolean> resultHandler) {
		byte[] encrypted = getEncryptedKey();
		cryptoExecutor.execute(() -> {
			byte[] key = crypto.decryptWithPassword(encrypted, password);
			if (key == null) {
				resultHandler.onResult(false);
			} else {
				accountManager.setDatabaseKey(new SecretKey(key));
				resultHandler.onResult(true);
			}
		});
	}

	@Override
	public void changePassword(String password, String newPassword,
			ResultHandler<Boolean> resultHandler) {
		byte[] encrypted = getEncryptedKey();
		cryptoExecutor.execute(() -> {
			byte[] key = crypto.decryptWithPassword(encrypted, password);
			if (key == null) {
				resultHandler.onResult(false);
			} else {
				String hex =
						encryptDatabaseKey(new SecretKey(key), newPassword);
				boolean stored = accountManager.storeEncryptedDatabaseKey(hex);
				resultHandler.onResult(stored);
			}
		});
	}

	private byte[] getEncryptedKey() {
		String hex = accountManager.getEncryptedDatabaseKey();
		if (hex == null)
			throw new IllegalStateException("Encrypted database key is null");
		return StringUtils.fromHexString(hex);
	}

	@CryptoExecutor
	String encryptDatabaseKey(SecretKey key, String password) {
		long start = now();
		byte[] encrypted = crypto.encryptWithPassword(key.getBytes(), password);
		logDuration(LOG, "Key derivation", start);
		return StringUtils.toHexString(encrypted);
	}
}

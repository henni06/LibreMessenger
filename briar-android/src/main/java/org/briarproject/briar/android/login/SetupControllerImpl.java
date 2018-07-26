package org.briarproject.briar.android.login;

import android.support.annotation.Nullable;

import org.briarproject.bramble.api.account.AccountManager;
import org.briarproject.bramble.api.crypto.CryptoComponent;
import org.briarproject.bramble.api.crypto.CryptoExecutor;
import org.briarproject.bramble.api.crypto.PasswordStrengthEstimator;
import org.briarproject.bramble.api.crypto.SecretKey;
import org.briarproject.bramble.api.identity.IdentityManager;
import org.briarproject.bramble.api.identity.LocalAuthor;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.briar.android.controller.handler.ResultHandler;
import org.briarproject.briar.android.controller.handler.UiResultHandler;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

@NotNullByDefault
public class SetupControllerImpl extends PasswordControllerImpl
		implements SetupController {

	private static final Logger LOG =
			Logger.getLogger(SetupControllerImpl.class.getName());

	private final IdentityManager identityManager;

	@Nullable
	private volatile SetupActivity setupActivity;

	@Inject
	SetupControllerImpl(AccountManager accountManager,
			@CryptoExecutor Executor cryptoExecutor, CryptoComponent crypto,
			PasswordStrengthEstimator strengthEstimator,
			IdentityManager identityManager) {
		super(accountManager, cryptoExecutor, crypto, strengthEstimator);
		this.identityManager = identityManager;
	}

	@Override
	public void setSetupActivity(SetupActivity setupActivity) {
		this.setupActivity = setupActivity;
	}

	@Override
	public boolean needToShowDozeFragment() {
		SetupActivity setupActivity = this.setupActivity;
		if (setupActivity == null) throw new IllegalStateException();
		return DozeView.needsToBeShown(setupActivity) ||
				HuaweiView.needsToBeShown(setupActivity);
	}

	@Override
	public void setAuthorName(String authorName) {
		SetupActivity setupActivity = this.setupActivity;
		if (setupActivity == null) throw new IllegalStateException();
		setupActivity.setAuthorName(authorName);
	}

	@Override
	public void setPassword(String password) {
		SetupActivity setupActivity = this.setupActivity;
		if (setupActivity == null) throw new IllegalStateException();
		setupActivity.setPassword(password);
	}

	@Override
	public void showPasswordFragment() {
		SetupActivity setupActivity = this.setupActivity;
		if (setupActivity == null) throw new IllegalStateException();
		setupActivity.showPasswordFragment();
	}

	@Override
	public void showDozeFragment() {
		SetupActivity setupActivity = this.setupActivity;
		if (setupActivity == null) throw new IllegalStateException();
		setupActivity.showDozeFragment();
	}

	@Override
	public void createAccount() {
		SetupActivity setupActivity = this.setupActivity;
		UiResultHandler<Void> resultHandler =
				new UiResultHandler<Void>(setupActivity) {
					@Override
					public void onResultUi(Void result) {
						if (setupActivity == null)
							throw new IllegalStateException();
						setupActivity.showApp();
					}
				};
		createAccount(resultHandler);
	}

	// Package access for testing
	void createAccount(ResultHandler<Void> resultHandler) {
		SetupActivity setupActivity = this.setupActivity;
		if (setupActivity == null) throw new IllegalStateException();
		String authorName = setupActivity.getAuthorName();
		if (authorName == null) throw new IllegalStateException();
		String password = setupActivity.getPassword();
		if (password == null) throw new IllegalStateException();
		cryptoExecutor.execute(() -> {
			LOG.info("Creating account");
			LocalAuthor localAuthor =
					identityManager.createLocalAuthor(authorName);
			identityManager.registerLocalAuthor(localAuthor);
			SecretKey key = crypto.generateSecretKey();
			String hex = encryptDatabaseKey(key, password);
			storeEncryptedDatabaseKey(hex);
			accountManager.setDatabaseKey(key);
			resultHandler.onResult(null);
		});
	}
}

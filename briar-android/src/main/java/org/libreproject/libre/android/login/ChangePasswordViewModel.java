package org.libreproject.libre.android.login;

import org.libreproject.bramble.api.account.AccountManager;
import org.libreproject.bramble.api.crypto.DecryptionException;
import org.libreproject.bramble.api.crypto.DecryptionResult;
import org.libreproject.bramble.api.crypto.PasswordStrengthEstimator;
import org.libreproject.bramble.api.lifecycle.IoExecutor;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.android.viewmodel.LiveEvent;
import org.libreproject.libre.android.viewmodel.MutableLiveEvent;

import java.util.concurrent.Executor;

import javax.inject.Inject;

import androidx.lifecycle.ViewModel;

import static org.libreproject.bramble.api.crypto.DecryptionResult.SUCCESS;

@NotNullByDefault
public class ChangePasswordViewModel extends ViewModel {

	private final AccountManager accountManager;
	private final Executor ioExecutor;
	private final PasswordStrengthEstimator strengthEstimator;

	@Inject
	ChangePasswordViewModel(AccountManager accountManager,
			@IoExecutor Executor ioExecutor,
			PasswordStrengthEstimator strengthEstimator) {
		this.accountManager = accountManager;
		this.ioExecutor = ioExecutor;
		this.strengthEstimator = strengthEstimator;
	}

	float estimatePasswordStrength(String password) {
		return strengthEstimator.estimateStrength(password);
	}

	LiveEvent<DecryptionResult> changePassword(String oldPassword,
			String newPassword) {
		MutableLiveEvent<DecryptionResult> result = new MutableLiveEvent<>();
		ioExecutor.execute(() -> {
			try {
				accountManager.changePassword(oldPassword, newPassword);
				result.postEvent(SUCCESS);
			} catch (DecryptionException e) {
				result.postEvent(e.getDecryptionResult());
			}
		});
		return result;
	}
}

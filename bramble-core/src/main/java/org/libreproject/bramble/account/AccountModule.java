package org.libreproject.bramble.account;

import org.libreproject.bramble.api.account.AccountManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AccountModule {

	@Provides
	@Singleton
	AccountManager provideAccountManager(AccountManagerImpl accountManager) {
		return accountManager;
	}
}

package org.libreproject.bramble.mailbox;

import org.libreproject.bramble.api.mailbox.MailboxSettingsManager;

import dagger.Module;
import dagger.Provides;

@Module
public class MailboxModule {

	@Provides
	MailboxSettingsManager provideMailboxSettingsManager(
			MailboxSettingsManagerImpl mailboxSettingsManager) {
		return mailboxSettingsManager;
	}
}

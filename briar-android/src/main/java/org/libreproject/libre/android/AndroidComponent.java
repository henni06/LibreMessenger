package org.libreproject.libre.android;

import org.libreproject.bramble.BrambleAndroidEagerSingletons;
import org.libreproject.bramble.BrambleAndroidModule;
import org.libreproject.bramble.BrambleAppComponent;
import org.libreproject.bramble.BrambleCoreEagerSingletons;
import org.libreproject.bramble.BrambleCoreModule;
import org.libreproject.bramble.account.BriarAccountModule;
import org.libreproject.bramble.api.FeatureFlags;
import org.libreproject.bramble.api.account.AccountManager;
import org.libreproject.bramble.api.connection.ConnectionRegistry;
import org.libreproject.bramble.api.contact.ContactExchangeManager;
import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.crypto.CryptoExecutor;
import org.libreproject.bramble.api.crypto.PasswordStrengthEstimator;
import org.libreproject.bramble.api.db.DatabaseExecutor;
import org.libreproject.bramble.api.db.TransactionManager;
import org.libreproject.bramble.api.event.EventBus;
import org.libreproject.bramble.api.identity.IdentityManager;
import org.libreproject.bramble.api.keyagreement.KeyAgreementTask;
import org.libreproject.bramble.api.keyagreement.PayloadEncoder;
import org.libreproject.bramble.api.keyagreement.PayloadParser;
import org.libreproject.bramble.api.lifecycle.IoExecutor;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.plugin.PluginManager;
import org.libreproject.bramble.api.settings.SettingsManager;
import org.libreproject.bramble.api.system.AndroidExecutor;
import org.libreproject.bramble.api.system.AndroidWakeLockManager;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.bramble.api.system.LocationUtils;
import org.libreproject.bramble.plugin.file.RemovableDriveModule;
import org.libreproject.bramble.plugin.tor.CircumventionProvider;
import org.libreproject.bramble.system.ClockModule;
import org.libreproject.libre.BriarCoreEagerSingletons;
import org.libreproject.libre.BriarCoreModule;
import org.libreproject.libre.android.attachment.AttachmentModule;
import org.libreproject.libre.android.attachment.media.MediaModule;
import org.libreproject.libre.android.contact.connect.BluetoothIntroFragment;
import org.libreproject.libre.android.conversation.glide.BriarModelLoader;
import org.libreproject.libre.android.hotspot.AbstractTabsFragment;
import org.libreproject.libre.android.hotspot.FallbackFragment;
import org.libreproject.libre.android.hotspot.HotspotIntroFragment;
import org.libreproject.libre.android.hotspot.ManualHotspotFragment;
import org.libreproject.libre.android.hotspot.QrHotspotFragment;
import org.libreproject.libre.android.logging.CachingLogHandler;
import org.libreproject.libre.android.login.SignInReminderReceiver;
import org.libreproject.libre.android.removabledrive.ChooserFragment;
import org.libreproject.libre.android.removabledrive.ReceiveFragment;
import org.libreproject.libre.android.removabledrive.SendFragment;
import org.libreproject.libre.android.settings.ConnectionsFragment;
import org.libreproject.libre.android.settings.NotificationsFragment;
import org.libreproject.libre.android.settings.SecurityFragment;
import org.libreproject.libre.android.settings.SettingsFragment;
import org.libreproject.libre.android.view.EmojiTextInputView;
import org.libreproject.libre.api.android.AndroidNotificationManager;
import org.libreproject.libre.api.android.DozeWatchdog;
import org.libreproject.libre.api.android.LockManager;
import org.libreproject.libre.api.android.ScreenFilterMonitor;
import org.libreproject.libre.api.attachment.AttachmentReader;
import org.libreproject.libre.api.autodelete.AutoDeleteManager;
import org.libreproject.libre.api.blog.BlogManager;
import org.libreproject.libre.api.blog.BlogPostFactory;
import org.libreproject.libre.api.blog.BlogSharingManager;
import org.libreproject.libre.api.client.MessageTracker;
import org.libreproject.libre.api.conversation.ConversationManager;
import org.libreproject.libre.api.feed.FeedManager;
import org.libreproject.libre.api.forum.ForumManager;
import org.libreproject.libre.api.forum.ForumSharingManager;
import org.libreproject.libre.api.identity.AuthorManager;
import org.libreproject.libre.api.introduction.IntroductionManager;
import org.libreproject.libre.api.messaging.MessagingManager;
import org.libreproject.libre.api.messaging.PrivateMessageFactory;
import org.libreproject.libre.api.privategroup.GroupMessageFactory;
import org.libreproject.libre.api.privategroup.PrivateGroupFactory;
import org.libreproject.libre.api.privategroup.PrivateGroupManager;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationFactory;
import org.libreproject.libre.api.privategroup.invitation.GroupInvitationManager;
import org.libreproject.libre.api.test.TestDataCreator;

import java.util.concurrent.Executor;

import javax.inject.Singleton;

import androidx.lifecycle.ViewModelProvider;
import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreModule.class,
		BriarCoreModule.class,
		BrambleAndroidModule.class,
		BriarAccountModule.class,
		AppModule.class,
		AttachmentModule.class,
		ClockModule.class,
		MediaModule.class,
		RemovableDriveModule.class
})
public interface AndroidComponent
		extends BrambleCoreEagerSingletons, BrambleAndroidEagerSingletons,
		BriarCoreEagerSingletons, AndroidEagerSingletons, BrambleAppComponent{

	// Exposed objects
	@CryptoExecutor
	Executor cryptoExecutor();

	PasswordStrengthEstimator passwordStrengthIndicator();

	@DatabaseExecutor
	Executor databaseExecutor();

	TransactionManager transactionManager();

	MessageTracker messageTracker();

	LifecycleManager lifecycleManager();

	IdentityManager identityManager();

	AttachmentReader attachmentReader();

	AuthorManager authorManager();

	PluginManager pluginManager();

	EventBus eventBus();

	AndroidNotificationManager androidNotificationManager();

	ScreenFilterMonitor screenFilterMonitor();

	ConnectionRegistry connectionRegistry();

	ContactManager contactManager();

	ConversationManager conversationManager();

	MessagingManager messagingManager();

	PrivateMessageFactory privateMessageFactory();

	PrivateGroupManager privateGroupManager();

	GroupInvitationFactory groupInvitationFactory();

	GroupInvitationManager groupInvitationManager();

	PrivateGroupFactory privateGroupFactory();

	GroupMessageFactory groupMessageFactory();

	ForumManager forumManager();

	ForumSharingManager forumSharingManager();

	BlogSharingManager blogSharingManager();

	BlogManager blogManager();

	BlogPostFactory blogPostFactory();

	SettingsManager settingsManager();

	ContactExchangeManager contactExchangeManager();

	KeyAgreementTask keyAgreementTask();

	PayloadEncoder payloadEncoder();

	PayloadParser payloadParser();

	IntroductionManager introductionManager();

	AndroidExecutor androidExecutor();

	FeedManager feedManager();

	Clock clock();

	TestDataCreator testDataCreator();

	DozeWatchdog dozeWatchdog();

	@IoExecutor
	Executor ioExecutor();

	AccountManager accountManager();

	LockManager lockManager();

	LocationUtils locationUtils();

	CircumventionProvider circumventionProvider();

	ViewModelProvider.Factory viewModelFactory();

	FeatureFlags featureFlags();

	AndroidWakeLockManager wakeLockManager();

	CachingLogHandler logHandler();

	Thread.UncaughtExceptionHandler exceptionHandler();

	AutoDeleteManager autoDeleteManager();


	void inject(SignInReminderReceiver briarService);

	void inject(BriarService briarService);

	void inject(NotificationCleanupService notificationCleanupService);

	void inject(EmojiTextInputView textInputView);

	void inject(BriarModelLoader briarModelLoader);

	void inject(SettingsFragment settingsFragment);

	void inject(ConnectionsFragment connectionsFragment);

	void inject(SecurityFragment securityFragment);

	void inject(NotificationsFragment notificationsFragment);

	void inject(HotspotIntroFragment hotspotIntroFragment);

	void inject(AbstractTabsFragment abstractTabsFragment);

	void inject(QrHotspotFragment qrHotspotFragment);

	void inject(ManualHotspotFragment manualHotspotFragment);

	void inject(FallbackFragment fallbackFragment);

	void inject(ChooserFragment chooserFragment);

	void inject(SendFragment sendFragment);

	void inject(ReceiveFragment receiveFragment);

	void inject(BluetoothIntroFragment bluetoothIntroFragment);
}

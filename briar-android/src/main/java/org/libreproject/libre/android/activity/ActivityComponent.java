package org.libreproject.libre.android.activity;

import android.app.Activity;

import org.libreproject.libre.android.AndroidComponent;
import org.libreproject.libre.android.StartupFailureActivity;
import org.libreproject.libre.android.account.SetupActivity;
import org.libreproject.libre.android.account.SetupFragment;
import org.libreproject.libre.android.account.UnlockActivity;
import org.libreproject.libre.android.blog.BlogActivity;
import org.libreproject.libre.android.blog.BlogFragment;
import org.libreproject.libre.android.blog.BlogPostFragment;
import org.libreproject.libre.android.blog.FeedFragment;
import org.libreproject.libre.android.blog.ReblogActivity;
import org.libreproject.libre.android.blog.ReblogFragment;
import org.libreproject.libre.android.blog.RssFeedActivity;
import org.libreproject.libre.android.blog.RssFeedDeleteFeedDialogFragment;
import org.libreproject.libre.android.blog.RssFeedImportFailedDialogFragment;
import org.libreproject.libre.android.blog.RssFeedImportFragment;
import org.libreproject.libre.android.blog.RssFeedManageFragment;
import org.libreproject.libre.android.blog.WriteBlogPostActivity;
import org.libreproject.libre.android.contact.ContactListFragment;
import org.libreproject.libre.android.contact.add.nearby.AddNearbyContactActivity;
import org.libreproject.libre.android.contact.add.nearby.AddNearbyContactErrorFragment;
import org.libreproject.libre.android.contact.add.nearby.AddNearbyContactFragment;
import org.libreproject.libre.android.contact.add.nearby.AddNearbyContactIntroFragment;
import org.libreproject.libre.android.contact.add.remote.AddContactActivity;
import org.libreproject.libre.android.contact.add.remote.LinkExchangeFragment;
import org.libreproject.libre.android.contact.add.remote.NicknameFragment;
import org.libreproject.libre.android.contact.add.remote.PendingContactListActivity;
import org.libreproject.libre.android.contact.connect.ConnectViaBluetoothActivity;
import org.libreproject.libre.android.conversation.AliasDialogFragment;
import org.libreproject.libre.android.conversation.ConversationActivity;
import org.libreproject.libre.android.conversation.ConversationSettingsDialog;
import org.libreproject.libre.android.conversation.ImageActivity;
import org.libreproject.libre.android.conversation.ImageFragment;
import org.libreproject.libre.android.forum.CreateForumActivity;
import org.libreproject.libre.android.forum.ForumActivity;
import org.libreproject.libre.android.forum.ForumListFragment;
import org.libreproject.libre.android.fragment.ScreenFilterDialogFragment;
import org.libreproject.libre.android.hotspot.HotspotActivity;
import org.libreproject.libre.android.introduction.ContactChooserFragment;
import org.libreproject.libre.android.introduction.IntroductionActivity;
import org.libreproject.libre.android.introduction.IntroductionMessageFragment;
import org.libreproject.libre.android.login.ChangePasswordActivity;
import org.libreproject.libre.android.login.OpenDatabaseFragment;
import org.libreproject.libre.android.login.PasswordFragment;
import org.libreproject.libre.android.login.StartupActivity;
import org.libreproject.libre.android.navdrawer.NavDrawerActivity;
import org.libreproject.libre.android.navdrawer.TransportsActivity;
import org.libreproject.libre.android.panic.PanicPreferencesActivity;
import org.libreproject.libre.android.panic.PanicResponderActivity;
import org.libreproject.libre.android.privategroup.conversation.GroupActivity;
import org.libreproject.libre.android.privategroup.creation.CreateGroupActivity;
import org.libreproject.libre.android.privategroup.creation.CreateGroupFragment;
import org.libreproject.libre.android.privategroup.creation.CreateGroupModule;
import org.libreproject.libre.android.privategroup.creation.GroupInviteActivity;
import org.libreproject.libre.android.privategroup.creation.GroupInviteFragment;
import org.libreproject.libre.android.privategroup.invitation.GroupInvitationActivity;
import org.libreproject.libre.android.privategroup.invitation.GroupInvitationModule;
import org.libreproject.libre.android.privategroup.list.GroupListFragment;
import org.libreproject.libre.android.privategroup.memberlist.GroupMemberListActivity;
import org.libreproject.libre.android.privategroup.memberlist.GroupMemberModule;
import org.libreproject.libre.android.privategroup.reveal.GroupRevealModule;
import org.libreproject.libre.android.privategroup.reveal.RevealContactsActivity;
import org.libreproject.libre.android.privategroup.reveal.RevealContactsFragment;
import org.libreproject.libre.android.removabledrive.RemovableDriveActivity;
import org.libreproject.libre.android.reporting.CrashFragment;
import org.libreproject.libre.android.reporting.CrashReportActivity;
import org.libreproject.libre.android.reporting.ReportFormFragment;
import org.libreproject.libre.android.settings.ConfirmAvatarDialogFragment;
import org.libreproject.libre.android.settings.SettingsActivity;
import org.libreproject.libre.android.settings.SettingsFragment;
import org.libreproject.libre.android.sharing.BlogInvitationActivity;
import org.libreproject.libre.android.sharing.BlogSharingStatusActivity;
import org.libreproject.libre.android.sharing.ForumInvitationActivity;
import org.libreproject.libre.android.sharing.ForumSharingStatusActivity;
import org.libreproject.libre.android.sharing.ShareBlogActivity;
import org.libreproject.libre.android.sharing.ShareBlogFragment;
import org.libreproject.libre.android.sharing.ShareForumActivity;
import org.libreproject.libre.android.sharing.ShareForumFragment;
import org.libreproject.libre.android.sharing.SharingModule;
import org.libreproject.libre.android.splash.SplashScreenActivity;
import org.libreproject.libre.android.test.TestDataActivity;

import dagger.Component;

@ActivityScope
@Component(modules = {
		ActivityModule.class,
		CreateGroupModule.class,
		GroupInvitationModule.class,
		GroupMemberModule.class,
		GroupRevealModule.class,
		SharingModule.SharingLegacyModule.class
}, dependencies = AndroidComponent.class)
public interface ActivityComponent {

	Activity activity();

	void inject(SplashScreenActivity activity);

	void inject(StartupActivity activity);

	void inject(SetupActivity activity);

	void inject(NavDrawerActivity activity);

	void inject(PanicResponderActivity activity);

	void inject(PanicPreferencesActivity activity);

	void inject(AddNearbyContactActivity activity);

	void inject(ConversationActivity activity);

	void inject(ImageActivity activity);

	void inject(ForumInvitationActivity activity);

	void inject(BlogInvitationActivity activity);

	void inject(CreateGroupActivity activity);

	void inject(GroupActivity activity);

	void inject(GroupInviteActivity activity);

	void inject(GroupInvitationActivity activity);

	void inject(GroupMemberListActivity activity);

	void inject(RevealContactsActivity activity);

	void inject(CreateForumActivity activity);

	void inject(ShareForumActivity activity);

	void inject(ShareBlogActivity activity);

	void inject(ForumSharingStatusActivity activity);

	void inject(BlogSharingStatusActivity activity);

	void inject(ForumActivity activity);

	void inject(BlogActivity activity);

	void inject(WriteBlogPostActivity activity);

	void inject(BlogFragment fragment);

	void inject(BlogPostFragment fragment);

	void inject(ReblogFragment fragment);

	void inject(ReblogActivity activity);

	void inject(SettingsActivity activity);

	void inject(TransportsActivity activity);

	void inject(TestDataActivity activity);

	void inject(ChangePasswordActivity activity);

	void inject(IntroductionActivity activity);

	void inject(RssFeedActivity activity);

	void inject(StartupFailureActivity activity);

	void inject(UnlockActivity activity);

	void inject(AddContactActivity activity);

	void inject(PendingContactListActivity activity);

	void inject(CrashReportActivity crashReportActivity);

	void inject(HotspotActivity hotspotActivity);

	void inject(RemovableDriveActivity activity);

	// Fragments

	void inject(SetupFragment fragment);

	void inject(PasswordFragment imageFragment);

	void inject(OpenDatabaseFragment activity);

	void inject(ContactListFragment fragment);

	void inject(CreateGroupFragment fragment);

	void inject(GroupListFragment fragment);

	void inject(GroupInviteFragment fragment);

	void inject(RevealContactsFragment activity);

	void inject(ForumListFragment fragment);

	void inject(FeedFragment fragment);

	void inject(AddNearbyContactIntroFragment fragment);

	void inject(AddNearbyContactFragment fragment);

	void inject(LinkExchangeFragment fragment);

	void inject(NicknameFragment fragment);

	void inject(ContactChooserFragment fragment);

	void inject(ShareForumFragment fragment);

	void inject(ShareBlogFragment fragment);

	void inject(IntroductionMessageFragment fragment);

	void inject(SettingsFragment fragment);

	void inject(ScreenFilterDialogFragment fragment);

	void inject(AddNearbyContactErrorFragment fragment);

	void inject(AliasDialogFragment aliasDialogFragment);

	void inject(ImageFragment imageFragment);

	void inject(ReportFormFragment reportFormFragment);

	void inject(CrashFragment crashFragment);

	void inject(ConfirmAvatarDialogFragment fragment);

	void inject(ConversationSettingsDialog dialog);

	void inject(RssFeedImportFragment fragment);

	void inject(RssFeedManageFragment fragment);

	void inject(RssFeedImportFailedDialogFragment fragment);

	void inject(RssFeedDeleteFeedDialogFragment fragment);

	void inject(ConnectViaBluetoothActivity connectViaBluetoothActivity);
}

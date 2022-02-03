package org.libreproject.libre.android.privategroup.memberlist;

import org.libreproject.libre.android.activity.ActivityScope;

import dagger.Module;
import dagger.Provides;

@Module
public class GroupMemberModule {

	@ActivityScope
	@Provides
	GroupMemberListController provideGroupMemberListController(
			GroupMemberListControllerImpl groupMemberListController) {
		return groupMemberListController;
	}
}

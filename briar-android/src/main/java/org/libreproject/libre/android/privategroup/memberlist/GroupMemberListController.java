package org.libreproject.libre.android.privategroup.memberlist;

import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.android.controller.DbController;
import org.libreproject.libre.android.controller.handler.ResultExceptionHandler;

import java.util.Collection;

public interface GroupMemberListController extends DbController {

	void loadMembers(GroupId groupId,
			ResultExceptionHandler<Collection<MemberListItem>, DbException> handler);

}

package org.libreproject.libre.android.sharing;

import org.libreproject.bramble.api.contact.Contact;
import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.contact.ContactManager;
import org.libreproject.bramble.api.db.DatabaseExecutor;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.NoSuchContactException;
import org.libreproject.bramble.api.db.NoSuchGroupException;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.android.contactselection.ContactSelectorControllerImpl;
import org.libreproject.libre.android.controller.handler.ExceptionHandler;
import org.libreproject.libre.api.forum.ForumSharingManager;
import org.libreproject.libre.api.identity.AuthorManager;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.libreproject.bramble.util.LogUtils.logException;

@Immutable
@NotNullByDefault
class ShareForumControllerImpl extends ContactSelectorControllerImpl
		implements ShareForumController {

	private final static Logger LOG =
			getLogger(ShareForumControllerImpl.class.getName());

	private final ForumSharingManager forumSharingManager;

	@Inject
	ShareForumControllerImpl(@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager, ContactManager contactManager,
			AuthorManager authorManager,
			ForumSharingManager forumSharingManager) {
		super(dbExecutor, lifecycleManager, contactManager, authorManager);
		this.forumSharingManager = forumSharingManager;
	}

	@Override
	protected boolean isDisabled(GroupId g, Contact c) throws DbException {
		return !forumSharingManager.canBeShared(g, c);
	}

	@Override
	public void share(GroupId g, Collection<ContactId> contacts,
			@Nullable String text, ExceptionHandler<DbException> handler) {
		runOnDbThread(() -> {
			try {
				for (ContactId c : contacts) {
					try {
						forumSharingManager.sendInvitation(g, c, text);
					} catch (NoSuchContactException | NoSuchGroupException e) {
						logException(LOG, WARNING, e);
					}
				}
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				handler.onException(e);
			}
		});
	}

}

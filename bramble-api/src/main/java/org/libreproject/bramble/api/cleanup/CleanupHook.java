package org.libreproject.bramble.api.cleanup;

import org.libreproject.bramble.api.db.DatabaseComponent;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.MessageId;

import java.util.Collection;

/**
 * An interface for registering a hook with the {@link CleanupManager}
 * that will be called when a message's cleanup deadline is reached.
 */
@NotNullByDefault
public interface CleanupHook {

	/**
	 * Called when the cleanup deadlines of one or more messages are reached.
	 * <p>
	 * The callee is not required to delete the messages, but the hook won't be
	 * called again for these messages unless another cleanup timer is set (see
	 * {@link DatabaseComponent#setCleanupTimerDuration(Transaction, MessageId, long)}
	 * and {@link DatabaseComponent#startCleanupTimer(Transaction, MessageId)}).
	 */
	void deleteMessages(Transaction txn, GroupId g,
			Collection<MessageId> messageIds) throws DbException;
}

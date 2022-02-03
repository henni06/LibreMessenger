package org.libreproject.libre.android.controller;

import org.libreproject.bramble.api.db.DatabaseExecutor;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
@Deprecated
@NotNullByDefault
public class DbControllerImpl implements DbController {

	private static final Logger LOG =
			Logger.getLogger(DbControllerImpl.class.getName());

	protected final Executor dbExecutor;
	private final LifecycleManager lifecycleManager;

	@Inject
	public DbControllerImpl(@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager) {
		this.dbExecutor = dbExecutor;
		this.lifecycleManager = lifecycleManager;
	}

	@Override
	public void runOnDbThread(Runnable task) {
		dbExecutor.execute(() -> {
			try {
				lifecycleManager.waitForDatabase();
				task.run();
			} catch (InterruptedException e) {
				LOG.warning("Interrupted while waiting for database");
				Thread.currentThread().interrupt();
			}
		});
	}
}

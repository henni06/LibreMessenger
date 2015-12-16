package org.briarproject.android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import org.briarproject.android.BriarService.BriarBinder;
import org.briarproject.android.BriarService.BriarServiceConnection;
import org.briarproject.api.db.DatabaseConfig;
import org.briarproject.api.db.DatabaseExecutor;
import org.briarproject.api.lifecycle.LifecycleManager;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

@SuppressLint("Registered")
public class BriarActivity extends BaseActivity {

	public static final int REQUEST_PASSWORD = 1;

	private static final Logger LOG =
			Logger.getLogger(BriarActivity.class.getName());

	private final BriarServiceConnection serviceConnection =
			new BriarServiceConnection();

	@Inject private DatabaseConfig databaseConfig;
	private boolean bound = false;

	// Fields that are accessed from background threads must be volatile
	@Inject @DatabaseExecutor private volatile Executor dbExecutor;
	@Inject private volatile LifecycleManager lifecycleManager;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		if (databaseConfig.getEncryptionKey() != null) startAndBindService();
	}

	@Override
	protected void onActivityResult(int request, int result, Intent data) {
		super.onActivityResult(request, result, data);
		if (request == REQUEST_PASSWORD) {
			if (result == RESULT_OK) startAndBindService();
			else finish();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (databaseConfig.getEncryptionKey() == null && !isFinishing()) {
			Intent i = new Intent(this, PasswordActivity.class);
			i.setFlags(FLAG_ACTIVITY_NO_ANIMATION | FLAG_ACTIVITY_SINGLE_TOP);
			startActivityForResult(i, REQUEST_PASSWORD);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService();
	}

	private void startAndBindService() {
		startService(new Intent(this, BriarService.class));
		bound = bindService(new Intent(this, BriarService.class),
				serviceConnection, 0);
	}

	private void unbindService() {
		if (bound) unbindService(serviceConnection);
	}

	protected void signOut() {
		new Thread() {
			@Override
			public void run() {
				try {
					// Wait for the service to finish starting up
					IBinder binder = serviceConnection.waitForBinder();
					BriarService service = ((BriarBinder) binder).getService();
					service.waitForStartup();
					// Shut down the service and wait for it to shut down
					LOG.info("Shutting down service");
					service.shutdown();
					service.waitForShutdown();
				} catch (InterruptedException e) {
					LOG.warning("Interrupted while waiting for service");
					Thread.currentThread().interrupt();
				}
				finishAndExit();
			}
		}.start();
	}

	private void finishAndExit() {
		runOnUiThread(new Runnable() {
			public void run() {
				finish();
				LOG.info("Exiting");
				System.exit(0);
			}
		});
	}

	protected void runOnDbThread(final Runnable task) {
		dbExecutor.execute(new Runnable() {
			public void run() {
				try {
					lifecycleManager.waitForDatabase();
					task.run();
				} catch (InterruptedException e) {
					LOG.warning("Interrupted while waiting for database");
					Thread.currentThread().interrupt();
				}
			}
		});
	}

	protected void finishOnUiThread() {
		runOnUiThread(new Runnable() {
			public void run() {
				finish();
			}
		});
	}
}

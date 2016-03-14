package org.briarproject.android.invitation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.briarproject.R;
import org.briarproject.android.AndroidComponent;
import org.briarproject.android.BriarActivity;
import org.briarproject.api.android.ReferenceManager;
import org.briarproject.api.crypto.CryptoComponent;
import org.briarproject.api.db.DbException;
import org.briarproject.api.identity.AuthorId;
import org.briarproject.api.identity.IdentityManager;
import org.briarproject.api.identity.LocalAuthor;
import org.briarproject.api.invitation.InvitationListener;
import org.briarproject.api.invitation.InvitationState;
import org.briarproject.api.invitation.InvitationTask;
import org.briarproject.api.invitation.InvitationTaskFactory;

import java.util.Collection;
import java.util.logging.Logger;

import javax.inject.Inject;

import static android.widget.Toast.LENGTH_LONG;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static org.briarproject.android.invitation.ConfirmationCodeView.ConfirmationState.CONNECTED;
import static org.briarproject.android.invitation.ConfirmationCodeView.ConfirmationState.DETAILS;
import static org.briarproject.android.invitation.ConfirmationCodeView.ConfirmationState.WAIT_FOR_CONTACT;

public class AddContactActivity extends BriarActivity
implements InvitationListener {

	static final int REQUEST_BLUETOOTH = 1;
	static final int REQUEST_CREATE_IDENTITY = 2;

	private static final Logger LOG =
			Logger.getLogger(AddContactActivity.class.getName());

	@Inject protected CryptoComponent crypto;
	@Inject protected InvitationTaskFactory invitationTaskFactory;
	@Inject protected ReferenceManager referenceManager;
	private AddContactView view = null;
	private InvitationTask task = null;
	private long taskHandle = -1;
	private AuthorId localAuthorId = null;
	private int localInvitationCode = -1, remoteInvitationCode = -1;
	private int localConfirmationCode = -1, remoteConfirmationCode = -1;
	private boolean connected = false, connectionFailed = false;
	private boolean localCompared = false, remoteCompared = false;
	private boolean localMatched = false, remoteMatched = false;
	private String contactName = null;

	// Fields that are accessed from background threads must be volatile
	@Inject protected volatile IdentityManager identityManager;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		if (state == null) {
			// This is a new activity
			setView(new ChooseIdentityView(this));
		} else {
			// Restore the activity's state
			byte[] b = state.getByteArray("briar.LOCAL_AUTHOR_ID");
			if (b != null) localAuthorId = new AuthorId(b);
			taskHandle = state.getLong("briar.TASK_HANDLE", -1);
			task = referenceManager.getReference(taskHandle,
					InvitationTask.class);

			if (task == null) {
				// No background task - we must be in an initial or final state
				localInvitationCode = state.getInt("briar.LOCAL_CODE");
				remoteInvitationCode = state.getInt("briar.REMOTE_CODE");
				connectionFailed = state.getBoolean("briar.FAILED");
				contactName = state.getString("briar.CONTACT_NAME");
				if (contactName != null) {
					localCompared = remoteCompared = true;
					localMatched = remoteMatched = true;
				}
				// Set the appropriate view for the state
				if (localInvitationCode == -1) {
					setView(new ChooseIdentityView(this));
				} else if (remoteInvitationCode == -1) {
					setView(new InvitationCodeView(this));
				} else if (connectionFailed) {
					setView(new ErrorView(this, R.string.connection_failed,
							R.string.could_not_find_contact));
				} else if (contactName == null) {
					setView(new ErrorView(this, R.string.codes_do_not_match,
							R.string.interfering));
				} else {
					showToastAndFinish();
				}
			} else {
				// A background task exists - listen to it and get its state
				InvitationState s = task.addListener(this);
				localInvitationCode = s.getLocalInvitationCode();
				remoteInvitationCode = s.getRemoteInvitationCode();
				localConfirmationCode = s.getLocalConfirmationCode();
				remoteConfirmationCode = s.getRemoteConfirmationCode();
				connected = s.getConnected();
				connectionFailed = s.getConnectionFailed();
				localCompared = s.getLocalCompared();
				remoteCompared = s.getRemoteCompared();
				localMatched = s.getLocalMatched();
				remoteMatched = s.getRemoteMatched();
				contactName = s.getContactName();
				// Set the appropriate view for the state
				if (localInvitationCode == -1) {
					setView(new ChooseIdentityView(this));
				} else if (remoteInvitationCode == -1) {
					setView(new InvitationCodeView(this));
				} else if (connectionFailed) {
					setView(new ErrorView(AddContactActivity.this,
							R.string.connection_failed,
							R.string.could_not_find_contact));
				} else if (connected && localConfirmationCode == -1) {
					setView(new ConfirmationCodeView(this, CONNECTED));
				} else if (localConfirmationCode == -1) {
					setView(new InvitationCodeView(this, true));
				} else if (!localCompared) {
					setView(new ConfirmationCodeView(this));
				} else if (!remoteCompared) {
					setView(new ConfirmationCodeView(this, WAIT_FOR_CONTACT));
				} else if (localMatched && remoteMatched) {
					if (contactName == null) {
						setView(new ConfirmationCodeView(this, DETAILS));
					} else {
						showToastAndFinish();
					}
				} else {
					setView(new ErrorView(this, R.string.codes_do_not_match,
							R.string.interfering));
				}
			}
		}
	}

	@Override
	public void injectActivity(AndroidComponent component) {
		component.inject(this);
	}

	private void showToastAndFinish() {
		String format = getString(R.string.contact_added_toast);
		String text = String.format(format, contactName);
		Toast.makeText(this, text, LENGTH_LONG).show();
		finish();
	}

	@Override
	public void onResume() {
		super.onResume();
		view.populate();
	}

	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		if (localAuthorId != null) {
			byte[] b = localAuthorId.getBytes();
			state.putByteArray("briar.LOCAL_AUTHOR_ID", b);
		}
		state.putInt("briar.LOCAL_CODE", localInvitationCode);
		state.putInt("briar.REMOTE_CODE", remoteInvitationCode);
		state.putBoolean("briar.FAILED", connectionFailed);
		state.putString("briar.CONTACT_NAME", contactName);
		if (task != null) state.putLong("briar.TASK_HANDLE", taskHandle);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (task != null) task.removeListener(this);
	}

	@Override
	public void onActivityResult(int request, int result, Intent data) {
		if (request == REQUEST_BLUETOOTH) {
			if (result != RESULT_CANCELED) reset(new InvitationCodeView(this));
		} else if (request == REQUEST_CREATE_IDENTITY && result == RESULT_OK) {
			byte[] b = data.getByteArrayExtra("briar.LOCAL_AUTHOR_ID");
			if (b == null) throw new IllegalStateException();
			localAuthorId = new AuthorId(b);
			setView(new ChooseIdentityView(this));
		}
	}

	void setView(AddContactView view) {
		this.view = view;
		view.init(this);
		setContentView(view);
	}

	void reset(AddContactView view) {
		// Don't reset localAuthorId
		task = null;
		taskHandle = -1;
		localInvitationCode = -1;
		localConfirmationCode = remoteConfirmationCode = -1;
		connected = connectionFailed = false;
		localCompared = remoteCompared = false;
		localMatched = remoteMatched = false;
		contactName = null;
		setView(view);
	}

	void loadLocalAuthors() {
		runOnDbThread(new Runnable() {
			public void run() {
				try {
					long now = System.currentTimeMillis();
					Collection<LocalAuthor> authors =
							identityManager.getLocalAuthors();
					long duration = System.currentTimeMillis() - now;
					if (LOG.isLoggable(INFO))
						LOG.info("Loading authors took " + duration + " ms");
					displayLocalAuthors(authors);
				} catch (DbException e) {
					if (LOG.isLoggable(WARNING))
						LOG.log(WARNING, e.toString(), e);
				}
			}
		});
	}

	// FIXME: The interaction between views and the container is horrible
	private void displayLocalAuthors(final Collection<LocalAuthor> authors) {
		runOnUiThread(new Runnable() {
			public void run() {
				AddContactView view = AddContactActivity.this.view;
				if (view instanceof ChooseIdentityView)
					((ChooseIdentityView) view).displayLocalAuthors(authors);
			}
		});
	}

	void setLocalAuthorId(AuthorId localAuthorId) {
		this.localAuthorId = localAuthorId;
	}

	AuthorId getLocalAuthorId() {
		return localAuthorId;
	}

	int getLocalInvitationCode() {
		if (localInvitationCode == -1)
			localInvitationCode = crypto.generateBTInvitationCode();
		return localInvitationCode;
	}

	int getRemoteInvitationCode() {
		return remoteInvitationCode;
	}

	void remoteInvitationCodeEntered(int code) {
		if (localAuthorId == null) throw new IllegalStateException();
		if (localInvitationCode == -1) throw new IllegalStateException();
		remoteInvitationCode = code;

		// change UI to show a progress indicator
		setView(new InvitationCodeView(this, true));

		task = invitationTaskFactory.createTask(localAuthorId,
				localInvitationCode, code);
		taskHandle = referenceManager.putReference(task, InvitationTask.class);
		task.addListener(AddContactActivity.this);
		// Add a second listener so we can remove the first in onDestroy(),
		// allowing the activity to be garbage collected if it's destroyed
		task.addListener(new ReferenceCleaner(referenceManager, taskHandle));
		task.connect();
	}

	int getLocalConfirmationCode() {
		return localConfirmationCode;
	}

	void remoteConfirmationCodeEntered(int code) {
		localCompared = true;
		if (code == remoteConfirmationCode) {
			localMatched = true;
			if (remoteMatched) {
				setView(new ConfirmationCodeView(this, DETAILS));
			} else if (remoteCompared) {
				setView(new ErrorView(this, R.string.codes_do_not_match,
						R.string.interfering));
			} else {
				setView(new ConfirmationCodeView(this, WAIT_FOR_CONTACT));
			}
			task.localConfirmationSucceeded();
		} else {
			localMatched = false;
			setView(new ErrorView(this, R.string.codes_do_not_match,
					R.string.interfering));
			task.localConfirmationFailed();
		}
	}

	public void connectionSucceeded() {
		runOnUiThread(new Runnable() {
			public void run() {
				connected = true;
				setView(new ConfirmationCodeView(AddContactActivity.this,
						CONNECTED));
			}
		});
	}

	public void connectionFailed() {
		runOnUiThread(new Runnable() {
			public void run() {
				connectionFailed = true;
				setView(new ErrorView(AddContactActivity.this,
						R.string.connection_failed,
						R.string.could_not_find_contact));
			}
		});
	}

	public void keyAgreementSucceeded(final int localCode,
			final int remoteCode) {
		runOnUiThread(new Runnable() {
			public void run() {
				localConfirmationCode = localCode;
				remoteConfirmationCode = remoteCode;
				setView(new ConfirmationCodeView(AddContactActivity.this));
			}
		});
	}

	public void keyAgreementFailed() {
		runOnUiThread(new Runnable() {
			public void run() {
				connectionFailed = true;
				setView(new ErrorView(AddContactActivity.this,
						R.string.connection_failed,
						R.string.could_not_find_contact));
			}
		});
	}

	public void remoteConfirmationSucceeded() {
		runOnUiThread(new Runnable() {
			public void run() {
				remoteCompared = true;
				remoteMatched = true;
				if (localMatched) {
					setView(new ConfirmationCodeView(AddContactActivity.this,
							DETAILS));
				}
			}
		});
	}

	public void remoteConfirmationFailed() {
		runOnUiThread(new Runnable() {
			public void run() {
				remoteCompared = true;
				remoteMatched = false;
				if (localMatched) {
					setView(new ErrorView(AddContactActivity.this,
							R.string.codes_do_not_match, R.string.interfering));
				}
			}
		});
	}

	public void pseudonymExchangeSucceeded(final String remoteName) {
		runOnUiThread(new Runnable() {
			public void run() {
				contactName = remoteName;
				showToastAndFinish();
			}
		});
	}

	public void pseudonymExchangeFailed() {
		runOnUiThread(new Runnable() {
			public void run() {
				setView(new ErrorView(AddContactActivity.this,
						R.string.connection_failed,
						R.string.could_not_find_contact));
			}
		});
	}

	/**
	 * Cleans up the reference to the invitation task when the task completes.
	 * This class is static to prevent memory leaks.
	 */
	private static class ReferenceCleaner implements InvitationListener {

		private final ReferenceManager referenceManager;
		private final long handle;

		private ReferenceCleaner(ReferenceManager referenceManager,
				long handle) {
			this.referenceManager = referenceManager;
			this.handle = handle;
		}

		public void connectionSucceeded() {
			// Wait for key agreement to succeed or fail
		}

		public void connectionFailed() {
			referenceManager.removeReference(handle, InvitationTask.class);
		}

		public void keyAgreementSucceeded(int localCode, int remoteCode) {
			// Wait for remote confirmation to succeed or fail
		}

		public void keyAgreementFailed() {
			referenceManager.removeReference(handle, InvitationTask.class);
		}

		public void remoteConfirmationSucceeded() {
			// Wait for the pseudonym exchange to succeed or fail
		}

		public void remoteConfirmationFailed() {
			referenceManager.removeReference(handle, InvitationTask.class);
		}

		public void pseudonymExchangeSucceeded(String remoteName) {
			referenceManager.removeReference(handle, InvitationTask.class);
		}

		public void pseudonymExchangeFailed() {
			referenceManager.removeReference(handle, InvitationTask.class);
		}
	}
}

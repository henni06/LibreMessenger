package org.libreproject.libre.android.blog;

import android.app.Application;
import android.util.Patterns;

import org.libreproject.bramble.api.db.DatabaseExecutor;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.db.TransactionManager;
import org.libreproject.bramble.api.lifecycle.IoExecutor;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.system.AndroidExecutor;
import org.libreproject.libre.android.viewmodel.DbViewModel;
import org.libreproject.libre.android.viewmodel.LiveEvent;
import org.libreproject.libre.android.viewmodel.LiveResult;
import org.libreproject.libre.android.viewmodel.MutableLiveEvent;
import org.libreproject.libre.api.feed.Feed;
import org.libreproject.libre.api.feed.FeedManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.libreproject.bramble.util.LogUtils.logDuration;
import static org.libreproject.bramble.util.LogUtils.logException;
import static org.libreproject.bramble.util.LogUtils.now;
import static org.libreproject.libre.android.blog.RssFeedViewModel.ImportResult.EXISTS;
import static org.libreproject.libre.android.blog.RssFeedViewModel.ImportResult.FAILED;
import static org.libreproject.libre.android.blog.RssFeedViewModel.ImportResult.IMPORTED;

@NotNullByDefault
class RssFeedViewModel extends DbViewModel {
	enum ImportResult {IMPORTED, FAILED, EXISTS}

	private static final Logger LOG =
			getLogger(RssFeedViewModel.class.getName());

	private final FeedManager feedManager;
	private final Executor ioExecutor;
	private final Executor dbExecutor;

	private final MutableLiveData<LiveResult<List<Feed>>> feeds =
			new MutableLiveData<>();

	@Nullable
	private volatile String urlFailedImport = null;
	private final MutableLiveData<Boolean> isImporting =
			new MutableLiveData<>(false);
	private final MutableLiveEvent<ImportResult> importResult =
			new MutableLiveEvent<>();

	@Inject
	RssFeedViewModel(Application app,
			FeedManager feedManager,
			@IoExecutor Executor ioExecutor,
			@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager,
			TransactionManager db,
			AndroidExecutor androidExecutor) {
		super(app, dbExecutor, lifecycleManager, db, androidExecutor);
		this.feedManager = feedManager;
		this.ioExecutor = ioExecutor;
		this.dbExecutor = dbExecutor;

		loadFeeds();
	}

	@Nullable
	String validateAndNormaliseUrl(String url) {
		if (!Patterns.WEB_URL.matcher(url).matches()) return null;
		try {
			return new URL(url).toString();
		} catch (MalformedURLException e) {
			return null;
		}
	}

	LiveData<LiveResult<List<Feed>>> getFeeds() {
		return feeds;
	}

	private void loadFeeds() {
		loadFromDb(this::loadFeeds, feeds::setValue);
	}

	@DatabaseExecutor
	private List<Feed> loadFeeds(Transaction txn) throws DbException {
		long start = now();
		List<Feed> feeds = feedManager.getFeeds(txn);
		Collections.sort(feeds);
		logDuration(LOG, "Loading feeds", start);
		return feeds;
	}

	void removeFeed(GroupId groupId) {
		dbExecutor.execute(() -> {
			List<Feed> updated = removeListItems(getList(feeds), feed -> {
				if (feed.getBlogId().equals(groupId)) {
					try {
						feedManager.removeFeed(feed);
						return true;
					} catch (DbException e) {
						handleException(e);
					}
				}
				return false;
			});
			if (updated != null) {
				feeds.postValue(new LiveResult<>(updated));
			}
		});
	}

	LiveEvent<ImportResult> getImportResult() {
		return importResult;
	}

	LiveData<Boolean> getIsImporting() {
		return isImporting;
	}

	void importFeed(String url) {
		isImporting.setValue(true);
		urlFailedImport = null;
		ioExecutor.execute(() -> {
			try {
				if (exists(url)) {
					importResult.postEvent(EXISTS);
					return;
				}
				Feed feed = feedManager.addFeed(url);
				List<Feed> updated = addListItem(getList(feeds), feed);
				if (updated != null) {
					Collections.sort(updated);
					feeds.postValue(new LiveResult<>(updated));
				}
				importResult.postEvent(IMPORTED);
			} catch (DbException | IOException e) {
				logException(LOG, WARNING, e);
				urlFailedImport = url;
				importResult.postEvent(FAILED);
			} finally {
				isImporting.postValue(false);
			}
		});
	}

	@Nullable
	String getUrlFailedImport() {
		return urlFailedImport;
	}

	private boolean exists(String url) {
		List<Feed> list = getList(feeds);
		if (list != null) {
			for (Feed feed : list) {
				if (url.equals(feed.getUrl())) {
					return true;
				}
			}
		}
		return false;
	}
}

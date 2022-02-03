package org.libreproject.libre.feed;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;

import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.client.ContactGroupFactory;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.data.BdfEntry;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.db.DatabaseComponent;
import org.libreproject.bramble.api.db.Transaction;
import org.libreproject.bramble.api.identity.LocalAuthor;
import org.libreproject.bramble.api.sync.Group;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.bramble.api.system.TaskScheduler;
import org.libreproject.bramble.test.BrambleMockTestCase;
import org.libreproject.bramble.test.DbExpectations;
import org.libreproject.bramble.test.ImmediateExecutor;
import org.libreproject.libre.api.blog.Blog;
import org.libreproject.libre.api.blog.BlogManager;
import org.libreproject.libre.api.blog.BlogPost;
import org.libreproject.libre.api.blog.BlogPostFactory;
import org.libreproject.libre.api.feed.Feed;
import org.jmock.Expectations;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.net.SocketFactory;

import okhttp3.Dns;
import okhttp3.OkHttpClient;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.libreproject.bramble.test.TestUtils.getGroup;
import static org.libreproject.bramble.test.TestUtils.getLocalAuthor;
import static org.libreproject.bramble.test.TestUtils.getMessage;
import static org.libreproject.libre.api.feed.FeedConstants.KEY_FEEDS;
import static org.libreproject.libre.api.feed.FeedManager.CLIENT_ID;
import static org.libreproject.libre.api.feed.FeedManager.MAJOR_VERSION;

public class FeedManagerImplTest extends BrambleMockTestCase {

	private final TaskScheduler scheduler = context.mock(TaskScheduler.class);
	private final Executor ioExecutor = new ImmediateExecutor();
	private final DatabaseComponent db = context.mock(DatabaseComponent.class);
	private final ContactGroupFactory contactGroupFactory =
			context.mock(ContactGroupFactory.class);
	private final ClientHelper clientHelper = context.mock(ClientHelper.class);
	private final BlogManager blogManager = context.mock(BlogManager.class);
	private final BlogPostFactory blogPostFactory =
			context.mock(BlogPostFactory.class);
	private final FeedFactory feedFactory = context.mock(FeedFactory.class);
	private final Clock clock = context.mock(Clock.class);
	private final Dns noDnsLookups = context.mock(Dns.class);

	private final OkHttpClient client = new OkHttpClient.Builder()
			.socketFactory(SocketFactory.getDefault())
			.dns(noDnsLookups)
			.connectTimeout(60_000, MILLISECONDS)
			.build();
	private final WeakSingletonProvider<OkHttpClient> httpClientProvider =
			new WeakSingletonProvider<OkHttpClient>() {
				@Override
				@Nonnull
				OkHttpClient createInstance() {
					return client;
				}
			};
	private final Group localGroup = getGroup(CLIENT_ID, MAJOR_VERSION);
	private final GroupId localGroupId = localGroup.getId();
	private final Group blogGroup =
			getGroup(BlogManager.CLIENT_ID, BlogManager.MAJOR_VERSION);
	private final GroupId blogGroupId = blogGroup.getId();
	private final LocalAuthor localAuthor = getLocalAuthor();
	private final Blog blog = new Blog(blogGroup, localAuthor, true);
	private final Feed feed =
			new Feed("http://example.org", blog, localAuthor, 0);
	private final BdfDictionary feedDict = new BdfDictionary();

	private final FeedManagerImpl feedManager =
			new FeedManagerImpl(scheduler, ioExecutor, db, contactGroupFactory,
					clientHelper, blogManager, blogPostFactory, feedFactory,
					httpClientProvider, clock);

	@Test
	public void testFetchFeedsReturnsEarlyIfTorIsNotActive() {
		feedManager.setTorActive(false);
		feedManager.fetchFeeds();
	}

	@Test
	public void testEmptyFetchFeeds() throws Exception {
		BdfList feedList = new BdfList();
		expectGetFeeds(feedList);
		expectStoreFeed(feedList);
		feedManager.setTorActive(true);
		feedManager.fetchFeeds();
	}

	@Test
	public void testFetchFeedsIoException() throws Exception {
		BdfDictionary feedDict = new BdfDictionary();
		BdfList feedList = BdfList.of(feedDict);

		expectGetFeeds(feedList);
		context.checking(new Expectations() {{
			oneOf(noDnsLookups).lookup("example.org");
			will(throwException(new UnknownHostException()));
		}});
		expectStoreFeed(feedList);

		feedManager.setTorActive(true);
		feedManager.fetchFeeds();
	}

	@Test
	public void testPostFeedEntriesEmptyDate() throws Exception {
		Transaction txn = new Transaction(null, false);
		List<SyndEntry> entries = new ArrayList<>();
		entries.add(new SyndEntryImpl());
		SyndEntry entry = new SyndEntryImpl();
		entry.setUpdatedDate(new Date());
		entries.add(entry);
		String text = "<p> (" + entry.getUpdatedDate().toString() + ")</p>";
		Message msg = getMessage(blogGroupId);
		BlogPost post = new BlogPost(msg, null, localAuthor);

		context.checking(new Expectations() {{
			oneOf(db).startTransaction(false);
			will(returnValue(txn));
			oneOf(clock).currentTimeMillis();
			will(returnValue(42L));
			oneOf(blogPostFactory).createBlogPost(feed.getBlogId(), 42L, null,
					localAuthor, text);
			will(returnValue(post));
			oneOf(blogManager).addLocalPost(txn, post);
			oneOf(db).commitTransaction(txn);
			oneOf(db).endTransaction(txn);
		}});
		feedManager.postFeedEntries(feed, entries);
	}

	private void expectGetLocalGroup() {
		context.checking(new Expectations() {{
			oneOf(contactGroupFactory).createLocalGroup(CLIENT_ID,
					MAJOR_VERSION);
			will(returnValue(localGroup));
		}});
	}

	private void expectGetFeeds(BdfList feedList) throws Exception {
		Transaction txn = new Transaction(null, true);
		BdfDictionary feedsDict =
				BdfDictionary.of(new BdfEntry(KEY_FEEDS, feedList));
		expectGetLocalGroup();
		context.checking(new DbExpectations() {{
			oneOf(db).transactionWithResult(with(true), withDbCallable(txn));
			oneOf(clientHelper).getGroupMetadataAsDictionary(txn, localGroupId);
			will(returnValue(feedsDict));
			if (feedList.size() == 1) {
				oneOf(feedFactory).createFeed(feedDict);
				will(returnValue(feed));
			}
		}});
	}

	private void expectStoreFeed(BdfList feedList) throws Exception {
		BdfDictionary feedDict =
				BdfDictionary.of(new BdfEntry(KEY_FEEDS, feedList));
		expectGetLocalGroup();
		context.checking(new Expectations() {{
			oneOf(clientHelper).mergeGroupMetadata(localGroupId, feedDict);
			if (feedList.size() == 1) {
				oneOf(feedFactory).feedToBdfDictionary(feed);
				will(returnValue(feedList.getDictionary(0)));
			}
		}});
	}

}

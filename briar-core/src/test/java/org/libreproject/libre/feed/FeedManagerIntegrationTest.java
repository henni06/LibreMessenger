package org.libreproject.libre.feed;

import org.libreproject.bramble.api.identity.Identity;
import org.libreproject.bramble.api.identity.IdentityManager;
import org.libreproject.bramble.api.lifecycle.LifecycleManager;
import org.libreproject.bramble.test.BrambleTestCase;
import org.libreproject.bramble.test.TestDatabaseConfigModule;
import org.libreproject.bramble.test.TestUtils;
import org.libreproject.libre.api.blog.Blog;
import org.libreproject.libre.api.blog.BlogManager;
import org.libreproject.libre.api.blog.BlogPostHeader;
import org.libreproject.libre.api.feed.Feed;
import org.libreproject.libre.api.feed.FeedManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collection;

import static org.libreproject.bramble.test.TestUtils.getSecretKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FeedManagerIntegrationTest extends BrambleTestCase {

	private LifecycleManager lifecycleManager;
	private FeedManager feedManager;
	private BlogManager blogManager;
	private final File testDir = TestUtils.getTestDirectory();
	private final File testFile = new File(testDir, "feedTest");

	@Before
	public void setUp() throws Exception {
		assertTrue(testDir.mkdirs());
		FeedManagerIntegrationTestComponent component =
				DaggerFeedManagerIntegrationTestComponent.builder()
						.testDatabaseConfigModule(
								new TestDatabaseConfigModule(testFile)).build();
		FeedManagerIntegrationTestComponent.Helper.injectEagerSingletons(component);
		component.inject(this);

		IdentityManager identityManager = component.getIdentityManager();
		Identity identity = identityManager.createIdentity("feedTest");
		identityManager.registerIdentity(identity);

		lifecycleManager = component.getLifecycleManager();
		lifecycleManager.startServices(getSecretKey());
		lifecycleManager.waitForStartup();

		feedManager = component.getFeedManager();
		blogManager = component.getBlogManager();
	}

	@Test
	public void testFeedImportAndRemoval() throws Exception {
		// initially, there's only the one personal blog
		Collection<Blog> blogs = blogManager.getBlogs();
		assertEquals(1, blogs.size());
		Blog personalBlog = blogs.iterator().next();

		// add feed into a dedicated blog
		String url = "https://www.schneier.com/blog/atom.xml";
		feedManager.addFeed(url);

		// then there's the feed's blog now
		blogs = blogManager.getBlogs();
		assertEquals(2, blogs.size());
		Blog feedBlog = null;
		for (Blog blog : blogs) {
			if (!blog.equals(personalBlog)) feedBlog = blog;
		}
		assertNotNull(feedBlog);

		// check the feed got saved as expected
		Collection<Feed> feeds = feedManager.getFeeds();
		assertEquals(1, feeds.size());
		Feed feed = feeds.iterator().next();
		assertTrue(feed.getLastEntryTime() > 0);
		assertTrue(feed.getAdded() > 0);
		assertTrue(feed.getUpdated() > 0);
		assertEquals(url, feed.getUrl());
		assertEquals(feedBlog, feed.getBlog());
		assertEquals("Schneier on Security", feed.getTitle());
		assertEquals(feed.getTitle(), feed.getBlog().getName());
		assertEquals(feed.getTitle(), feed.getLocalAuthor().getName());

		// check the feed entries have been added to the blog as expected
		Collection<BlogPostHeader> headers =
				blogManager.getPostHeaders(feedBlog.getId());
		for (BlogPostHeader header : headers) {
			assertTrue(header.isRssFeed());
		}

		// now let's remove the feed's blog again
		blogManager.removeBlog(feedBlog);
		blogs = blogManager.getBlogs();
		assertEquals(1, blogs.size());
		assertEquals(personalBlog, blogs.iterator().next());
		assertEquals(0, feedManager.getFeeds().size());
	}

	@After
	public void tearDown() throws Exception {
		lifecycleManager.stopServices();
		lifecycleManager.waitForShutdown();
		TestUtils.deleteTestDirectory(testDir);
	}
}

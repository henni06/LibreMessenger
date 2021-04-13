package org.briarproject.briar.forum;

import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.test.TestDatabaseConfigModule;
import org.briarproject.briar.api.forum.Forum;
import org.briarproject.briar.api.forum.ForumManager;
import org.briarproject.briar.api.forum.ForumPost;
import org.briarproject.briar.api.forum.ForumPostHeader;
import org.briarproject.briar.api.forum.ForumSharingManager;
import org.briarproject.briar.test.BriarIntegrationTest;
import org.briarproject.briar.test.BriarIntegrationTestComponent;
import org.briarproject.briar.test.DaggerBriarIntegrationTestComponent;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import javax.annotation.Nullable;

import static org.briarproject.briar.test.BriarTestUtils.assertGroupCount;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ForumManagerTest
		extends BriarIntegrationTest<BriarIntegrationTestComponent> {

	private ForumManager forumManager0, forumManager1;
	private ForumSharingManager forumSharingManager0, forumSharingManager1;
	private Forum forum0;
	private GroupId groupId0;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		forumManager0 = c0.getForumManager();
		forumManager1 = c1.getForumManager();
		forumSharingManager0 = c0.getForumSharingManager();
		forumSharingManager1 = c1.getForumSharingManager();


		forum0 = forumManager0.addForum("Test Forum");
		groupId0 = forum0.getId();
		// share forum
		forumSharingManager0.sendInvitation(groupId0, contactId1From0, null);
		sync0To1(1, true);
		forumSharingManager1.respondToInvitation(forum0, contact0From1, true);
		sync1To0(1, true);
	}

	@Override
	protected void createComponents() {
		BriarIntegrationTestComponent component =
				DaggerBriarIntegrationTestComponent.builder().build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(component);
		component.inject(this);

		c0 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseConfigModule(new TestDatabaseConfigModule(t0Dir))
				.build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(c0);

		c1 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseConfigModule(new TestDatabaseConfigModule(t1Dir))
				.build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(c1);

		c2 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseConfigModule(new TestDatabaseConfigModule(t2Dir))
				.build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(c2);
	}

	private ForumPost createForumPost(GroupId groupId,
			@Nullable ForumPost parent, String text, long ms) throws Exception {
		return forumPostFactory.createPost(groupId, ms,
				parent == null ? null : parent.getMessage().getId(),
				author0, text);
	}

	@Test
	public void testForumPost() throws Exception {
		assertEquals(1, forumManager0.getForums().size());
		long ms1 = c0.getClock().currentTimeMillis() - 1000L;
		String text1 = "some forum text";
		long ms2 = c0.getClock().currentTimeMillis();
		String text2 = "some other forum text";
		ForumPost post1 =
				createForumPost(forum0.getGroup().getId(), null, text1, ms1);
		assertEquals(ms1, post1.getMessage().getTimestamp());
		ForumPost post2 =
				createForumPost(forum0.getGroup().getId(), post1, text2, ms2);
		assertEquals(ms2, post2.getMessage().getTimestamp());
		forumManager0.addLocalPost(post1);
		forumManager0.setReadFlag(forum0.getGroup().getId(),
				post1.getMessage().getId(), true);
		assertGroupCount(messageTracker0, forum0.getGroup().getId(), 1, 0,
				post1.getMessage().getTimestamp());
		forumManager0.addLocalPost(post2);
		forumManager0.setReadFlag(forum0.getGroup().getId(),
				post2.getMessage().getId(), false);
		assertGroupCount(messageTracker0, forum0.getGroup().getId(), 2, 1,
				post2.getMessage().getTimestamp());
		forumManager0.setReadFlag(forum0.getGroup().getId(),
				post2.getMessage().getId(), false);
		assertGroupCount(messageTracker0, forum0.getGroup().getId(), 2, 1,
				post2.getMessage().getTimestamp());
		Collection<ForumPostHeader> headers =
				forumManager0.getPostHeaders(forum0.getGroup().getId());
		assertEquals(2, headers.size());
		for (ForumPostHeader h : headers) {
			String hText = forumManager0.getPostText(h.getId());

			boolean isPost1 = h.getId().equals(post1.getMessage().getId());
			boolean isPost2 = h.getId().equals(post2.getMessage().getId());
			assertTrue(isPost1 || isPost2);
			if (isPost1) {
				assertEquals(h.getTimestamp(), ms1);
				assertEquals(text1, hText);
				assertNull(h.getParentId());
				assertTrue(h.isRead());
			} else {
				assertEquals(h.getTimestamp(), ms2);
				assertEquals(text2, hText);
				assertEquals(h.getParentId(), post2.getParent());
				assertFalse(h.isRead());
			}
		}
		forumManager0.removeForum(forum0);
		assertEquals(0, forumManager0.getForums().size());
	}

	@Test
	public void testForumPostDelivery() throws Exception {
		// add one forum post
		long time = c0.getClock().currentTimeMillis();
		ForumPost post1 = createForumPost(groupId0, null, "a", time);
		forumManager0.addLocalPost(post1);
		assertEquals(1, forumManager0.getPostHeaders(groupId0).size());
		assertEquals(0, forumManager1.getPostHeaders(groupId0).size());
		assertGroupCount(messageTracker0, groupId0, 1, 0, time);
		assertGroupCount(messageTracker1, groupId0, 0, 0, 0);

		// send post to 1
		sync0To1(1, true);
		assertEquals(1, forumManager1.getPostHeaders(groupId0).size());
		assertGroupCount(messageTracker1, groupId0, 1, 1, time);

		// add another forum post
		long time2 = c0.getClock().currentTimeMillis();
		ForumPost post2 = createForumPost(groupId0, null, "b", time2);
		forumManager1.addLocalPost(post2);
		assertEquals(1, forumManager0.getPostHeaders(groupId0).size());
		assertEquals(2, forumManager1.getPostHeaders(groupId0).size());
		assertGroupCount(messageTracker0, groupId0, 1, 0, time);
		assertGroupCount(messageTracker1, groupId0, 2, 1, time2);

		// send post to 0
		sync1To0(1, true);
		assertEquals(2, forumManager1.getPostHeaders(groupId0).size());
		assertGroupCount(messageTracker0, groupId0, 2, 1, time2);
	}

	@Test
	public void testForumPostDeliveredAfterParent() throws Exception {
		// Add a parent post and a child post
		long time = c0.getClock().currentTimeMillis();
		ForumPost post1 = createForumPost(groupId0, null, "a", time);
		ForumPost post2 = createForumPost(groupId0, post1, "a", time);
		forumManager0.addLocalPost(post1);
		forumManager0.addLocalPost(post2);
		assertEquals(2, forumManager0.getPostHeaders(groupId0).size());
		assertEquals(0, forumManager1.getPostHeaders(groupId0).size());

		// Unshare the parent so it won't be sent yet
		setMessageNotShared(c0, post1.getMessage().getId());

		// Send the child post to 1 - it should be validated but not delivered
		// yet, as the parent is missing, and no headers should be returned yet
		syncMessage(c0, c1, contactId1From0, 1, 0, 1, 0);
		assertEquals(0, forumManager1.getPostHeaders(groupId0).size());

		// Now send the parent post to 1 - it should be validated, both
		// posts should be delivered, and both headers should be returned
		setMessageShared(c0, post1.getMessage().getId());
		syncMessage(c0, c1, contactId1From0, 1, 0, 0, 2);
		assertEquals(2, forumManager1.getPostHeaders(groupId0).size());
	}

	@Test
	public void testForumPostWithParentInOtherGroup() throws Exception {
		// share a second forum
		Forum forum1 = forumManager0.addForum("Test Forum1");
		GroupId g1 = forum1.getId();
		forumSharingManager0.sendInvitation(g1, contactId1From0, null);
		sync0To1(1, true);
		forumSharingManager1.respondToInvitation(forum1, contact0From1, true);
		sync1To0(1, true);

		// add one forum post with a parent in another forum
		long time = c0.getClock().currentTimeMillis();
		ForumPost post1 = createForumPost(g1, null, "a", time);
		ForumPost post = createForumPost(groupId0, post1, "b", time);
		forumManager0.addLocalPost(post);
		assertEquals(1, forumManager0.getPostHeaders(groupId0).size());
		assertEquals(0, forumManager1.getPostHeaders(groupId0).size());

		// send the child post to 1
		sync0To1(1, false);
		assertEquals(1, forumManager0.getPostHeaders(groupId0).size());
		assertEquals(0, forumManager1.getPostHeaders(groupId0).size());

		// now also add the parent post which is in another group
		forumManager0.addLocalPost(post1);
		assertEquals(1, forumManager0.getPostHeaders(g1).size());
		assertEquals(0, forumManager1.getPostHeaders(g1).size());

		// send posts to 1
		sync0To1(1, true);
		assertEquals(1, forumManager0.getPostHeaders(groupId0).size());
		assertEquals(1, forumManager0.getPostHeaders(g1).size());
		// the next line is critical, makes sure post doesn't show up
		assertEquals(0, forumManager1.getPostHeaders(groupId0).size());
		assertEquals(1, forumManager1.getPostHeaders(g1).size());
	}

}

package org.libreproject.libre.sharing;

import org.libreproject.bramble.api.db.DbException;
import org.libreproject.libre.api.blog.Blog;
import org.libreproject.libre.api.blog.BlogManager;
import org.libreproject.libre.api.blog.event.BlogInvitationResponseReceivedEvent;
import org.libreproject.libre.api.conversation.ConversationManager.ConversationClient;
import org.libreproject.libre.api.conversation.event.ConversationMessageReceivedEvent;
import org.libreproject.libre.api.sharing.InvitationResponse;
import org.libreproject.libre.api.sharing.Shareable;
import org.libreproject.libre.api.sharing.SharingManager;
import org.libreproject.libre.test.BriarIntegrationTestComponent;
import org.junit.Before;

import java.util.Collection;

public class AutoDeleteBlogIntegrationTest
		extends AbstractAutoDeleteIntegrationTest {

	private SharingManager<Blog> sharingManager0;
	private SharingManager<Blog> sharingManager1;
	private Blog shareable;
	private BlogManager manager0;
	private BlogManager manager1;
	private Class<BlogInvitationResponseReceivedEvent>
			responseReceivedEventClass;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		manager0 = c0.getBlogManager();
		manager1 = c1.getBlogManager();
		// personalBlog(author0) is already shared with c1
		shareable = manager0.getPersonalBlog(author2);
		sharingManager0 = c0.getBlogSharingManager();
		sharingManager1 = c1.getBlogSharingManager();
		responseReceivedEventClass = BlogInvitationResponseReceivedEvent.class;
	}

	@Override
	protected ConversationClient getConversationClient(
			BriarIntegrationTestComponent component) {
		return component.getBlogSharingManager();
	}

	@Override
	protected SharingManager<? extends Shareable> getSharingManager0() {
		return sharingManager0;
	}

	@Override
	protected SharingManager<? extends Shareable> getSharingManager1() {
		return sharingManager1;
	}

	@Override
	protected Shareable getShareable() {
		return shareable;
	}

	@Override
	protected Collection<Blog> subscriptions0() throws DbException {
		return manager0.getBlogs();
	}

	@Override
	protected Collection<Blog> subscriptions1() throws DbException {
		return manager1.getBlogs();
	}

	@Override
	protected Class<? extends ConversationMessageReceivedEvent<? extends InvitationResponse>> getResponseReceivedEventClass() {
		return responseReceivedEventClass;
	}
}

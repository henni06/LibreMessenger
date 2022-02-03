package org.libreproject.libre.headless.blogs

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import org.libreproject.bramble.api.db.DbException
import org.libreproject.bramble.api.db.TransactionManager
import org.libreproject.bramble.api.identity.IdentityManager
import org.libreproject.bramble.api.system.Clock
import org.libreproject.bramble.util.StringUtils.utf8IsTooLong
import org.libreproject.libre.api.blog.BlogConstants.MAX_BLOG_POST_TEXT_LENGTH
import org.libreproject.libre.api.blog.BlogManager
import org.libreproject.libre.api.blog.BlogPostFactory
import org.libreproject.libre.api.blog.BlogPostHeader
import org.libreproject.libre.headless.getFromJson
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
@Singleton
internal class BlogControllerImpl
@Inject
constructor(
    private val blogManager: BlogManager,
    private val blogPostFactory: BlogPostFactory,
    private val db: TransactionManager,
    private val identityManager: IdentityManager,
    private val objectMapper: ObjectMapper,
    private val clock: Clock
) : BlogController {

    override fun listPosts(ctx: Context): Context {
        val posts = blogManager.blogs
            .flatMap { blog -> blogManager.getPostHeaders(blog.id) }
            .asSequence()
            .sortedBy { it.timeReceived }
            .map { header -> header.output(blogManager.getPostText(header.id)) }
            .toList()
        return ctx.json(posts)
    }

    override fun createPost(ctx: Context): Context {
        val text = ctx.getFromJson(objectMapper, "text")
        if (utf8IsTooLong(text, MAX_BLOG_POST_TEXT_LENGTH))
            throw BadRequestResponse("Blog post text is too long")

        val author = identityManager.localAuthor
        val blog = blogManager.getPersonalBlog(author)
        val now = clock.currentTimeMillis()
        val post = blogPostFactory.createBlogPost(blog.id, now, null, author, text)
        val header = db.transactionWithResult<BlogPostHeader, DbException>(true) { txn ->
            blogManager.addLocalPost(txn, post)
            return@transactionWithResult blogManager.getPostHeader(txn, blog.id, post.message.id)
        }
        return ctx.json(header.output(text))
    }

}

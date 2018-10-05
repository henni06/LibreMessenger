package org.briarproject.briar.headless.forums

import io.javalin.BadRequestResponse
import io.javalin.Context
import org.briarproject.bramble.util.StringUtils
import org.briarproject.briar.api.forum.ForumConstants
import org.briarproject.briar.api.forum.ForumManager
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
@Singleton
internal class ForumControllerImpl
@Inject
constructor(private val forumManager: ForumManager) : ForumController {

    override fun list(ctx: Context): Context {
        return ctx.json(forumManager.forums.output())
    }

    override fun create(ctx: Context): Context {
        val name = ctx.formParam("text")
        if (name == null || name.isNullOrEmpty())
            throw BadRequestResponse("Expecting Forum Name")
        if (StringUtils.utf8IsTooLong(name, ForumConstants.MAX_FORUM_NAME_LENGTH))
            throw BadRequestResponse("Forum name is too long")
        return ctx.json(forumManager.addForum(name).output())
    }

}

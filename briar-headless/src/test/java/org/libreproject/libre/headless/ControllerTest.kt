package org.libreproject.libre.headless

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.http.Context
import io.javalin.http.util.ContextUtil
import io.mockk.mockk
import org.libreproject.bramble.api.connection.ConnectionRegistry
import org.libreproject.bramble.api.contact.Contact
import org.libreproject.bramble.api.contact.ContactManager
import org.libreproject.bramble.api.db.TransactionManager
import org.libreproject.bramble.api.identity.Author
import org.libreproject.bramble.api.identity.IdentityManager
import org.libreproject.bramble.api.identity.LocalAuthor
import org.libreproject.bramble.api.sync.Group
import org.libreproject.bramble.api.sync.Message
import org.libreproject.bramble.api.system.Clock
import org.libreproject.bramble.test.TestUtils.getAuthor
import org.libreproject.bramble.test.TestUtils.getClientId
import org.libreproject.bramble.test.TestUtils.getContact
import org.libreproject.bramble.test.TestUtils.getGroup
import org.libreproject.bramble.test.TestUtils.getLocalAuthor
import org.libreproject.bramble.test.TestUtils.getMessage
import org.libreproject.bramble.util.StringUtils.getRandomString
import org.libreproject.libre.api.conversation.ConversationManager
import org.libreproject.libre.headless.event.WebSocketController
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

abstract class ControllerTest {

    protected val db = mockk<TransactionManager>()
    protected val contactManager = mockk<ContactManager>()
    protected val conversationManager = mockk<ConversationManager>()
    protected val identityManager = mockk<IdentityManager>()
    protected val connectionRegistry = mockk<ConnectionRegistry>()
    protected val clock = mockk<Clock>()
    protected val ctx = mockk<Context>()

    protected val webSocketController = mockk<WebSocketController>()

    private val request = mockk<HttpServletRequest>(relaxed = true)
    private val response = mockk<HttpServletResponse>(relaxed = true)
    private val outputCtx = ContextUtil.init(request, response)

    protected val objectMapper = ObjectMapper()

    protected val group: Group = getGroup(getClientId(), 0)
    protected val author: Author = getAuthor()
    protected val localAuthor: LocalAuthor = getLocalAuthor()
    protected val contact: Contact = getContact(author, localAuthor.id, true)
    protected val message: Message = getMessage(group.id)
    protected val text: String = getRandomString(5)
    protected val timestamp = 42L
    protected val unreadCount = 42

    protected fun assertJsonEquals(json: String, obj: Any) {
        assertEquals(json, outputCtx.json(obj).resultString(), STRICT)
    }

}

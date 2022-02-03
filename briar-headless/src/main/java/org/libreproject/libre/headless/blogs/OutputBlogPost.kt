package org.libreproject.libre.headless.blogs

import org.libreproject.bramble.identity.output
import org.libreproject.libre.api.blog.BlogPostHeader
import org.libreproject.libre.api.blog.MessageType
import org.libreproject.libre.headless.json.JsonDict

internal fun BlogPostHeader.output(text: String) = JsonDict(
    "text" to text,
    "author" to author.output(),
    "authorStatus" to authorInfo.status.output(),
    "type" to type.output(),
    "id" to id.bytes,
    "parentId" to parentId?.bytes,
    "read" to isRead,
    "rssFeed" to isRssFeed,
    "timestamp" to timestamp,
    "timestampReceived" to timeReceived
)

internal fun MessageType.output() = name.toLowerCase()

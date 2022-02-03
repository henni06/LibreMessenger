package org.libreproject.libre.headless.forums

import org.libreproject.libre.api.forum.Forum
import org.libreproject.libre.headless.json.JsonDict

internal fun Forum.output() = JsonDict(
    "name" to name,
    "id" to id.bytes
)

internal fun Collection<Forum>.output() = map { it.output() }

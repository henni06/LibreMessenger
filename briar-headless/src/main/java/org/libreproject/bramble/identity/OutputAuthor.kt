package org.libreproject.bramble.identity

import org.libreproject.bramble.api.identity.Author
import org.libreproject.libre.api.identity.AuthorInfo
import org.libreproject.libre.headless.json.JsonDict

fun Author.output() = JsonDict(
    "formatVersion" to formatVersion,
    "id" to id.bytes,
    "name" to name,
    "publicKey" to publicKey.encoded
)

fun AuthorInfo.Status.output() = name.toLowerCase()

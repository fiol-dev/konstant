package io.github.fioldev.konfigure.sources.source

import io.github.fioldev.konfigure.core.ConfigSource
import io.github.fioldev.konfigure.core.KeyFormat
import io.github.fioldev.konfigure.sources.parser.DotEnvParser
import io.github.fioldev.konfigure.sources.readFileText

class DotEnvSource(path: String = ".env") : ConfigSource {
    override val keyFormat: KeyFormat = KeyFormat.SCREAMING_SNAKE
    private val entries: Map<String, String> = try {
        DotEnvParser.parse(readFileText(path))
    } catch (_: Exception) {
        emptyMap()
    }

    override fun get(key: String): String? = entries[key]
}
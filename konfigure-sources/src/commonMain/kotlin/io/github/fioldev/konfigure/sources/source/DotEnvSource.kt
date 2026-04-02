package io.github.fioldev.konfigure.sources.source

import io.github.fioldev.konfigure.core.ConfigSource
import io.github.fioldev.konfigure.core.KeyFormat
import io.github.fioldev.konfigure.sources.parser.DotEnvParser
import io.github.fioldev.konfigure.sources.readFileText

class DotEnvSource private constructor(
    private val entries: Map<String, String>,
) : ConfigSource {
    override val keyFormat: KeyFormat = KeyFormat.SCREAMING_SNAKE

    override fun get(key: String): String? = entries[key]

    companion object {
        /**
         * Load from a .env file. If the file does not exist, returns an empty source.
         * Parse errors in a successfully read file are propagated as exceptions.
         */
        operator fun invoke(path: String = ".env"): DotEnvSource {
            val content = try {
                readFileText(path)
            } catch (_: Exception) {
                // File not found — different platforms throw different exception types
                return DotEnvSource(emptyMap())
            }
            return DotEnvSource(DotEnvParser.parse(content))
        }

        /**
         * Create from an already-read string content.
         */
        fun fromString(content: String): DotEnvSource =
            DotEnvSource(DotEnvParser.parse(content))
    }
}
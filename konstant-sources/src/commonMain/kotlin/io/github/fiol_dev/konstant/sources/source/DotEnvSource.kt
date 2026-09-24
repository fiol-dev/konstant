@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.sources.source

import io.github.fiol_dev.konstant.core.ConfigSource
import io.github.fiol_dev.konstant.core.InternalKonstantApi
import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.sources.parser.DotEnvParser
import io.github.fiol_dev.konstant.sources.readFileText

public class DotEnvSource private constructor(
    private val entries: Map<String, String>,
) : ConfigSource {
    override val keyFormat: KeyFormat = KeyFormat.SCREAMING_SNAKE

    override fun get(key: String): String? = entries[key]

    public companion object {
        /**
         * Load from a .env file. If the file does not exist, returns an empty source.
         * Parse errors in a successfully read file are propagated as exceptions.
         */
        public operator fun invoke(path: String = ".env"): DotEnvSource {
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
        public fun fromString(content: String): DotEnvSource =
            DotEnvSource(DotEnvParser.parse(content))
    }
}
@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.core.ConfigSource
import io.github.fiol_dev.konstant.core.InternalKonstantApi
import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.sources.parser.DotEnvParser

public class DotEnvSource private constructor(
    private val entries: Map<String, String>,
) : ConfigSource {
    override val keyFormat: KeyFormat = KeyFormat.SCREAMING_SNAKE

    override fun get(key: String): String? = entries[key]

    public companion object {
        /**
         * Reads a `.env` file. A missing file gives an empty source unless [optional] is false,
         * since a local `.env` usually exists only on developer machines.
         */
        public fun fromFile(path: String = ".env", optional: Boolean = true): DotEnvSource {
            if (!optional) return fromString(readFileText(path))
            val content = try {
                readFileText(path)
            } catch (_: Exception) {
                // File not found: platforms throw different exception types
                return DotEnvSource(emptyMap())
            }
            return fromString(content)
        }

        /**
         * Reads a `.env` file bundled with the app, see [readResourceText] for where each platform
         * looks. With [optional] a missing file gives an empty source instead of an error.
         */
        public fun fromResource(path: String, optional: Boolean = false): DotEnvSource =
            fromString(resourceText(path, optional))

        /**
         * Create from an already-read string content.
         */
        public fun fromString(content: String): DotEnvSource =
            DotEnvSource(DotEnvParser.parse(content))
    }
}
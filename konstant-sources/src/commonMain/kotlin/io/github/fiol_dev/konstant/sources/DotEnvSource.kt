@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.core.ConfigSource
import io.github.fiol_dev.konstant.core.InternalKonstantApi
import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.sources.parser.DotEnvParser

/**
 * Reads a `.env` file of `KEY=value` lines, with keys in `SCREAMING_SNAKE_CASE`. Unlike the other
 * file sources, keys are matched exactly, including case.
 *
 * The parser supports blank lines, full-line `#` comments, a leading `export `, and ` # comments`
 * after unquoted values. Double-quoted values unescape `\n`, `\r`, `\t`, `\"` and `\\`;
 * single-quoted values are taken literally. Lines without `=` are skipped, and a later duplicate
 * key wins. Multiline values and `${VAR}` expansion are not supported.
 */
public class DotEnvSource private constructor(
    private val entries: Map<String, String>,
    private val origin: String?,
) : ConfigSource {
    override val keyFormat: KeyFormat = KeyFormat.SCREAMING_SNAKE

    override val name: String get() = if (origin.isNullOrEmpty()) "DotEnvSource" else "DotEnvSource($origin)"

    override fun get(key: String): String? = entries[key]

    public companion object {
        /**
         * Reads a `.env` file. A missing file gives an empty source unless [optional] is false,
         * since a local `.env` usually exists only on developer machines.
         */
        public fun fromFile(path: String = ".env", optional: Boolean = true): DotEnvSource {
            if (!optional) return fromString(readFileText(path), origin = path)
            val content = try {
                readFileText(path)
            } catch (_: Exception) {
                // File not found: platforms throw different exception types
                return DotEnvSource(emptyMap(), path)
            }
            return fromString(content, origin = path)
        }

        /**
         * Reads a `.env` file bundled with the app, see [readResourceText] for where each platform
         * looks. With [optional] a missing file gives an empty source instead of an error.
         */
        public fun fromResource(path: String, optional: Boolean = false): DotEnvSource =
            fromString(resourceText(path, optional), origin = path)

        /**
         * Parses `.env` text that is already in memory. [origin], such as the file name, labels
         * the source in `ConfigLoader.explain` reports.
         */
        public fun fromString(content: String, origin: String? = null): DotEnvSource =
            DotEnvSource(DotEnvParser.parse(content), origin)
    }
}
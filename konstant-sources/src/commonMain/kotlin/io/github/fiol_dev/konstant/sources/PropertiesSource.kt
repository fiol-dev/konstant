@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.core.InternalKonstantApi
import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.core.MapBackedSource
import io.github.fiol_dev.konstant.sources.parser.PropertiesParser

/**
 * Reads `.properties` files. Keys use `dot.notation`, with `SCREAMING_SNAKE_CASE` as a fallback;
 * a key with no exact match falls back to a case-insensitive one.
 *
 * The parser handles one `key=value` or `key: value` pair per line, split at the first `=` or
 * `:`, with surrounding whitespace trimmed. Lines starting with `#` or `!` are comments, and a
 * later duplicate key wins. It does not support whitespace as the separator, `\` escapes
 * (including `\uXXXX`) or line continuations.
 */
public class PropertiesSource(properties: Map<String, String>) : MapBackedSource(properties) {
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE)

    public companion object {
        /** Parses properties text that is already in memory. */
        public fun fromString(content: String): PropertiesSource =
            PropertiesSource(PropertiesParser.parse(content))

        /**
         * Reads the file at [path]. Throws if it cannot be read, as in browsers, which have no
         * file system; use [fromString] there.
         */
        public fun fromFile(path: String): PropertiesSource =
            fromString(readFileText(path))

        /**
         * Reads a file bundled with the app, see [readResourceText] for where each platform looks.
         * With [optional] a missing file gives an empty source instead of an error.
         */
        public fun fromResource(path: String, optional: Boolean = false): PropertiesSource =
            fromString(resourceText(path, optional))
    }
}

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
 * The parser follows the `java.util.Properties` basics: `key=value`, `key: value` or `key value`,
 * `#` and `!` comments, lines continued with a trailing `\`, and the escapes `\uXXXX`, `\t`, `\n`,
 * `\r`, `\f` and escaped separators. Values are trimmed, and a later duplicate key wins.
 */
public class PropertiesSource(properties: Map<String, String>, origin: String? = null) : MapBackedSource(properties, origin) {
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE)

    public companion object {
        /**
         * Parses properties text that is already in memory.
         * [origin], such as the file name, labels the source in `ConfigLoader.explain` reports.
         */
        public fun fromString(content: String, origin: String? = null): PropertiesSource =
            PropertiesSource(PropertiesParser.parse(content), origin)

        /**
         * Reads the file at [path]. Throws if it cannot be read, as in browsers, which have no
         * file system; use [fromString] there.
         */
        public fun fromFile(path: String): PropertiesSource =
            fromString(readFileText(path), origin = path)

        /**
         * Reads a file bundled with the app, see [readResourceText] for where each platform looks.
         * With [optional] a missing file gives an empty source instead of an error.
         */
        public fun fromResource(path: String, optional: Boolean = false): PropertiesSource =
            fromString(resourceText(path, optional), origin = path)
    }
}

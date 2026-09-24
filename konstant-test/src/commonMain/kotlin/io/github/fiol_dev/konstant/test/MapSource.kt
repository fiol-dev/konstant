package io.github.fiol_dev.konstant.test

import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.core.MapBackedSource

/**
 * An in-memory source for tests. Like the real sources, lookups fall back to a
 * case-insensitive match and `Map` fields read their entries through [children].
 */
public class MapSource(
    values: Map<String, String>,
    override val keyFormat: KeyFormat = KeyFormat.RAW,
) : MapBackedSource(values) {
    public companion object {
        public fun of(vararg pairs: Pair<String, String>): MapSource =
            MapSource(mapOf(*pairs))

        public fun screamingSnake(vararg pairs: Pair<String, String>): MapSource =
            MapSource(mapOf(*pairs), KeyFormat.SCREAMING_SNAKE)

        public fun dotNotation(vararg pairs: Pair<String, String>): MapSource =
            MapSource(mapOf(*pairs), KeyFormat.DOT_NOTATION)
    }
}

package io.github.fiol_dev.konstant.test

import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.core.MapBackedSource

/**
 * An in-memory source for tests. Like the file-based sources, lookups fall back to a
 * case-insensitive match and `Map` fields read their entries through [children].
 */
public class MapSource(
    values: Map<String, String>,
    override val keyFormat: KeyFormat = KeyFormat.RAW,
) : MapBackedSource(values) {
    public companion object {
        /**
         * A source whose keys are the property names as written ([KeyFormat.RAW]), such as
         * `port`. Keys of nested fields are the prefix plus the name: `DATABASE_maxPoolSize`.
         */
        public fun of(vararg pairs: Pair<String, String>): MapSource =
            MapSource(mapOf(*pairs))

        /** A source whose keys look like environment variables: `DATABASE_MAX_POOL_SIZE`. */
        public fun screamingSnake(vararg pairs: Pair<String, String>): MapSource =
            MapSource(mapOf(*pairs), KeyFormat.SCREAMING_SNAKE)

        /** A source whose keys look like a properties file: `database.max.pool.size`. */
        public fun dotNotation(vararg pairs: Pair<String, String>): MapSource =
            MapSource(mapOf(*pairs), KeyFormat.DOT_NOTATION)
    }
}

package io.github.fiol_dev.konstant.core

/**
 * How a field's property name becomes a lookup key in a [ConfigSource]. The examples are for the
 * field `maxPoolSize` inside a nested spec held in a property named `database`.
 */
public enum class KeyFormat {
    /** Upper case with underscores: `DATABASE_MAX_POOL_SIZE`. Used by environment variables. */
    SCREAMING_SNAKE,

    /**
     * Lower case with dots, including between camel-case words: `database.max.pool.size`. Used by
     * the TOML, YAML, JSON and properties sources.
     */
    DOT_NOTATION,

    /**
     * The property name unchanged. A nested prefix is still added in `SCREAMING_SNAKE` form:
     * `DATABASE_maxPoolSize`, or just `maxPoolSize` at the top level.
     */
    RAW
}

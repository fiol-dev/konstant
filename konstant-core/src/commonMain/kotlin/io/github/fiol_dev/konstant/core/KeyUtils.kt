package io.github.fiol_dev.konstant.core

/** Builds lookup keys from property names for [ConfigLoader.resolve] and generated code. */
@InternalKonstantApi
public object KeyUtils {

    /** `maxPoolSize` to `MAX_POOL_SIZE`. */
    public fun camelToScreamingSnake(name: String): String = buildString {
        for ((i, ch) in name.withIndex()) {
            if (ch.isUpperCase() && i > 0) append('_')
            append(ch.uppercaseChar())
        }
    }

    /** `maxPoolSize` to `max.pool.size`. */
    public fun camelToDotNotation(name: String): String = buildString {
        for ((i, ch) in name.withIndex()) {
            if (ch.isUpperCase() && i > 0) append('.')
            append(ch.lowercaseChar())
        }
    }

    /**
     * The key for [propertyName] in [format] under the `SCREAMING_SNAKE` [prefix], or [customKey]
     * unchanged when it is set.
     */
    public fun resolveKey(
        propertyName: String,
        prefix: String?,
        format: KeyFormat,
        customKey: String? = null,
    ): String {
        if (customKey != null) return customKey

        val formatted = when (format) {
            KeyFormat.SCREAMING_SNAKE -> camelToScreamingSnake(propertyName)
            KeyFormat.DOT_NOTATION -> camelToDotNotation(propertyName)
            KeyFormat.RAW -> propertyName
        }

        return when {
            prefix.isNullOrEmpty() -> formatted
            // Nested prefixes are SCREAMING_SNAKE ("OUTER_INNER"), so each segment becomes a dot level
            format == KeyFormat.DOT_NOTATION -> "${prefix.lowercase().replace('_', '.')}.$formatted"
            format == KeyFormat.SCREAMING_SNAKE -> "${prefix}_$formatted"
            else -> "${prefix}_$formatted"
        }
    }

    /** The `SCREAMING_SNAKE` prefix for a nested spec held in [propertyName], e.g. `OUTER_INNER`. */
    public fun resolvePrefix(
        propertyName: String,
        parentPrefix: String?,
    ): String {
        val segment = camelToScreamingSnake(propertyName)
        return if (parentPrefix.isNullOrEmpty()) segment else "${parentPrefix}_$segment"
    }
}

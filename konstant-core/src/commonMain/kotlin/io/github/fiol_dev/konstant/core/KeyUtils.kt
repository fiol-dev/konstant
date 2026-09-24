package io.github.fiol_dev.konstant.core

object KeyUtils {

    fun camelToScreamingSnake(name: String): String = buildString {
        for ((i, ch) in name.withIndex()) {
            if (ch.isUpperCase() && i > 0) append('_')
            append(ch.uppercaseChar())
        }
    }

    fun camelToDotNotation(name: String): String = buildString {
        for ((i, ch) in name.withIndex()) {
            if (ch.isUpperCase() && i > 0) append('.')
            append(ch.lowercaseChar())
        }
    }

    fun resolveKey(
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
            format == KeyFormat.DOT_NOTATION -> "${prefix.lowercase()}.$formatted"
            format == KeyFormat.SCREAMING_SNAKE -> "${prefix}_$formatted"
            else -> "${prefix}_$formatted"
        }
    }

    fun resolvePrefix(
        propertyName: String,
        parentPrefix: String?,
    ): String {
        val segment = camelToScreamingSnake(propertyName)
        return if (parentPrefix.isNullOrEmpty()) segment else "${parentPrefix}_$segment"
    }
}

package io.github.fiol_dev.konstant.core

/**
 * Why a key failed to load. The subclasses are plain classes rather than data classes, so
 * fields can be added later without breaking callers; they still compare by value.
 */
public sealed class ConfigError {
    /**
     * The key that failed, as looked up in the source that supplied the value. For a missing key
     * it is the `SCREAMING_SNAKE` form (or the `@Key` name). For a failed `init` check it is the
     * spec's prefix, such as `DATABASE` for a nested spec, or the class name when there is none.
     */
    public abstract val key: String

    /** A readable description of the problem, used in [ConfigException]'s message. */
    public abstract val message: String

    /** The values that [equals], [hashCode] and [toString] are based on. */
    internal abstract val parts: List<Pair<String, Any?>>

    override fun equals(other: Any?): Boolean =
        other != null && other::class == this::class && (other as ConfigError).parts == parts

    override fun hashCode(): Int = 31 * this::class.hashCode() + parts.hashCode()

    override fun toString(): String =
        "${this::class.simpleName}(${parts.joinToString { (name, value) -> "$name=$value" }})"

    /** No source had a value for [key], and the field has no default and is not nullable. */
    public class MissingRequired(
        override val key: String,
    ) : ConfigError() {
        override val parts: List<Pair<String, Any?>> get() = listOf("key" to key)

        override val message: String get() = "Required configuration key '$key' is missing"
    }

    /** The value for [key] could not be converted to the field's type. */
    public class ConversionFailed(
        override val key: String,
        /** The text read from the source, or `***` for a `@Secret` field. */
        public val rawValue: String,
        /** The field's type as written in the spec, e.g. `Int` or `List<Duration>`. */
        public val targetType: String,
        /**
         * The converter's exception message (`unknown` if it had none), or `invalid value` for
         * a `@Secret` field. A string, not the exception itself.
         */
        public val cause: String,
    ) : ConfigError() {
        override val parts: List<Pair<String, Any?>>
            get() = listOf("key" to key, "rawValue" to rawValue, "targetType" to targetType, "cause" to cause)

        override val message: String
            get() = "Failed to convert '$key' value '$rawValue' to $targetType: $cause"
    }

    /**
     * A value converted fine but broke a validation rule, or the config class's `init` block
     * rejected the loaded values. [rawValue] is null in the second case and `***` for secrets.
     */
    public class ValidationFailed(
        override val key: String,
        /** The text read from the source, `***` for a `@Secret` field, or null for an `init` check. */
        public val rawValue: String?,
        /** What rule was broken, e.g. `must be at least 1`, or the `require` message. */
        public val reason: String,
    ) : ConfigError() {
        override val parts: List<Pair<String, Any?>>
            get() = listOf("key" to key, "rawValue" to rawValue, "reason" to reason)

        override val message: String
            get() = if (rawValue == null) {
                "Invalid configuration '$key': $reason"
            } else {
                "Invalid value for '$key' ('$rawValue'): $reason"
            }
    }

    /**
     * Groups the [errors] of a nested config under [key]. Generated loaders never produce it:
     * they report a nested spec's errors directly, with the nested prefix in each key. It is
     * available for hand-written loaders.
     */
    public class NestedFailure(
        override val key: String,
        /** The errors inside the nested config. */
        public val errors: List<ConfigError>,
    ) : ConfigError() {
        override val parts: List<Pair<String, Any?>> get() = listOf("key" to key, "errors" to errors)

        override val message: String
            get() = "Nested config '$key' has ${errors.size} error(s): ${errors.joinToString { it.message }}"
    }
}

/**
 * Thrown by [ConfigResult.getOrThrow] and the generated `Konstant.init<Spec>` functions. The
 * message lists every error, one per line:
 *
 * ```
 * Configuration loading failed with 2 error(s):
 *   - Required configuration key 'DATABASE_URL' is missing
 *   - Failed to convert 'server.port' value 'abc' to Int: ...
 * ```
 */
public class ConfigException(
    /** The errors that caused the failure. */
    public val errors: List<ConfigError>,
) : RuntimeException(
    buildString {
        appendLine("Configuration loading failed with ${errors.size} error(s):")
        errors.forEach { appendLine("  - ${it.message}") }
    }
)

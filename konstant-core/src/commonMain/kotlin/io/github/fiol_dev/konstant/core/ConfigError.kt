package io.github.fiol_dev.konstant.core

/**
 * Why a key failed to load. The subclasses are plain classes rather than data classes, so
 * fields can be added later without breaking callers; they still compare by value.
 */
public sealed class ConfigError {
    public abstract val key: String
    public abstract val message: String

    /** The values that [equals], [hashCode] and [toString] are based on. */
    internal abstract val parts: List<Pair<String, Any?>>

    override fun equals(other: Any?): Boolean =
        other != null && other::class == this::class && (other as ConfigError).parts == parts

    override fun hashCode(): Int = 31 * this::class.hashCode() + parts.hashCode()

    override fun toString(): String =
        "${this::class.simpleName}(${parts.joinToString { (name, value) -> "$name=$value" }})"

    public class MissingRequired(
        override val key: String,
    ) : ConfigError() {
        override val parts: List<Pair<String, Any?>> get() = listOf("key" to key)

        override val message: String get() = "Required configuration key '$key' is missing"
    }

    public class ConversionFailed(
        override val key: String,
        public val rawValue: String,
        public val targetType: String,
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
        public val rawValue: String?,
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

    public class NestedFailure(
        override val key: String,
        public val errors: List<ConfigError>,
    ) : ConfigError() {
        override val parts: List<Pair<String, Any?>> get() = listOf("key" to key, "errors" to errors)

        override val message: String
            get() = "Nested config '$key' has ${errors.size} error(s): ${errors.joinToString { it.message }}"
    }
}

public class ConfigException(public val errors: List<ConfigError>) : RuntimeException(
    buildString {
        appendLine("Configuration loading failed with ${errors.size} error(s):")
        errors.forEach { appendLine("  - ${it.message}") }
    }
)

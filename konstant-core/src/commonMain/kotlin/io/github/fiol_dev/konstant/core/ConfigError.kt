package io.github.fiol_dev.konstant.core

public sealed class ConfigError {
    public abstract val key: String
    public abstract val message: String

    public data class MissingRequired(
        override val key: String,
    ) : ConfigError() {
        override val message: String get() = "Required configuration key '$key' is missing"
    }

    public data class ConversionFailed(
        override val key: String,
        val rawValue: String,
        val targetType: String,
        val cause: String,
    ) : ConfigError() {
        override val message: String
            get() = "Failed to convert '$key' value '$rawValue' to $targetType: $cause"
    }

    /**
     * A value converted fine but broke a validation rule, or the config class's `init` block
     * rejected the loaded values. [rawValue] is null in the second case and `***` for secrets.
     */
    public data class ValidationFailed(
        override val key: String,
        val rawValue: String?,
        val reason: String,
    ) : ConfigError() {
        override val message: String
            get() = if (rawValue == null) {
                "Invalid configuration '$key': $reason"
            } else {
                "Invalid value for '$key' ('$rawValue'): $reason"
            }
    }

    public data class NestedFailure(
        override val key: String,
        val errors: List<ConfigError>,
    ) : ConfigError() {
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

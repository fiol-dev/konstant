package io.github.fioldev.konfigure.core

sealed class ConfigError {
    abstract val key: String
    abstract val message: String

    data class MissingRequired(
        override val key: String,
    ) : ConfigError() {
        override val message: String get() = "Required configuration key '$key' is missing"
    }

    data class ConversionFailed(
        override val key: String,
        val rawValue: String,
        val targetType: String,
        val cause: String,
    ) : ConfigError() {
        override val message: String
            get() = "Failed to convert '$key' value '$rawValue' to $targetType: $cause"
    }

    data class NestedFailure(
        override val key: String,
        val errors: List<ConfigError>,
    ) : ConfigError() {
        override val message: String
            get() = "Nested config '$key' has ${errors.size} error(s): ${errors.joinToString { it.message }}"
    }
}

class ConfigException(val errors: List<ConfigError>) : RuntimeException(
    buildString {
        appendLine("Configuration loading failed with ${errors.size} error(s):")
        errors.forEach { appendLine("  - ${it.message}") }
    }
)

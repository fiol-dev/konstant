package io.github.fiol_dev.konstant.core

/**
 * A place config values are read from, such as environment variables or a file. For each field,
 * [ConfigLoader] builds the key in [keyFormat], then in each of [fallbackKeyFormats], and asks
 * [get] for each until one returns a value, before moving on to the next source.
 *
 * ```kotlin
 * class FlagSource(private val flags: Map<String, String>) : ConfigSource {
 *     override val keyFormat = KeyFormat.DOT_NOTATION
 *     override fun get(key: String): String? = flags[key]
 * }
 * ```
 */
public interface ConfigSource {
    /** The key format tried first, e.g. [KeyFormat.SCREAMING_SNAKE] for environment variables. */
    public val keyFormat: KeyFormat

    /** Further formats tried in order when the key in [keyFormat] has no value. None by default. */
    public val fallbackKeyFormats: List<KeyFormat> get() = emptyList()

    /** The raw text for [key], or null if this source does not have it. */
    public fun get(key: String): String?

    /** A short label for reports such as [ConfigLoader.explain], e.g. `TomlSource`. */
    public val name: String get() = this::class.simpleName ?: "ConfigSource"

    /**
     * Entries nested under [key] as `key.child`, keyed by `child`, or null if there are none.
     * Sources that flatten tables and mappings (TOML, YAML, properties) use this to fill
     * `Map` fields.
     */
    public fun children(key: String): Map<String, String>? = null
}

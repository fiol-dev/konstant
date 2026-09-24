package io.github.fiol_dev.konstant.core

public interface ConfigSource {
    public val keyFormat: KeyFormat
    public val fallbackKeyFormats: List<KeyFormat> get() = emptyList()
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

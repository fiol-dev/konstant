package io.github.fiol_dev.konstant.core

/**
 * Base [ConfigSource] backed by an in-memory map with case-insensitive fallback lookup.
 * [origin], such as a file path, is added to [name] so reports tell sources of one type apart:
 * `TomlSource(config/app.dev.toml)`.
 */
public abstract class MapBackedSource(
    private val entries: Map<String, String>,
    private val origin: String? = null,
) : ConfigSource {
    override val name: String
        get() {
            val type = this::class.simpleName ?: "ConfigSource"
            return if (origin.isNullOrEmpty()) type else "$type($origin)"
        }

    override fun get(key: String): String? =
        entries[key] ?: entries.entries.firstOrNull {
            it.key.equals(key, ignoreCase = true)
        }?.value

    override fun children(key: String): Map<String, String>? {
        val prefix = "$key."
        return entries
            .filterKeys { it.length > prefix.length && it.startsWith(prefix, ignoreCase = true) }
            .mapKeys { it.key.substring(prefix.length) }
            .takeIf { it.isNotEmpty() }
    }
}

package io.github.fiol_dev.konstant.core

/**
 * Base [ConfigSource] backed by an in-memory map with case-insensitive fallback lookup.
 */
public abstract class MapBackedSource(private val entries: Map<String, String>) : ConfigSource {
    override fun get(key: String): String? =
        entries[key] ?: entries.entries.firstOrNull {
            it.key.equals(key, ignoreCase = true)
        }?.value
}

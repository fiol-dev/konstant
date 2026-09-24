package io.github.fiol_dev.konstant.core

public interface ConfigSource {
    public val keyFormat: KeyFormat
    public val fallbackKeyFormats: List<KeyFormat> get() = emptyList()
    public fun get(key: String): String?
}

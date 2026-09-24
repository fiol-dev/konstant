package io.github.fiol_dev.konstant.core

interface ConfigSource {
    val keyFormat: KeyFormat
    val fallbackKeyFormats: List<KeyFormat> get() = emptyList()
    fun get(key: String): String?
}

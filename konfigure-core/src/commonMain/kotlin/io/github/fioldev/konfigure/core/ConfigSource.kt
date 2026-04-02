package io.github.fioldev.konfigure.core

interface ConfigSource {
    val keyFormat: KeyFormat
    val fallbackKeyFormats: List<KeyFormat> get() = emptyList()
    fun get(key: String): String?
}

package io.github.fioldev.konfigure.sources.source

import io.github.fioldev.konfigure.core.ConfigSource
import io.github.fioldev.konfigure.core.KeyFormat
import io.github.fioldev.konfigure.sources.parser.YamlParser
import io.github.fioldev.konfigure.sources.readFileText

class YamlSource(private val entries: Map<String, String>) : ConfigSource {
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE)

    override fun get(key: String): String? {
        return entries[key] ?: entries.entries.firstOrNull {
            it.key.equals(key, ignoreCase = true)
        }?.value
    }

    companion object {
        fun fromString(content: String): YamlSource =
            YamlSource(YamlParser.parse(content))

        fun fromFile(path: String): YamlSource =
            fromString(readFileText(path))
    }
}

fun loadYamlFile(path: String): YamlSource = YamlSource.fromFile(path)

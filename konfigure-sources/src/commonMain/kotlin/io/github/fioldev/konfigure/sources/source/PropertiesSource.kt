package io.github.fioldev.konfigure.sources.source

import io.github.fioldev.konfigure.core.ConfigSource
import io.github.fioldev.konfigure.core.KeyFormat
import io.github.fioldev.konfigure.sources.parser.PropertiesParser
import io.github.fioldev.konfigure.sources.readFileText

class PropertiesSource(private val properties: Map<String, String>) : ConfigSource {
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE)

    override fun get(key: String): String? {
        return properties[key] ?: properties.entries.firstOrNull {
            it.key.equals(key, ignoreCase = true)
        }?.value
    }

    companion object {
        fun fromString(content: String): PropertiesSource =
            PropertiesSource(PropertiesParser.parse(content))

        fun fromFile(path: String): PropertiesSource =
            fromString(readFileText(path))
    }
}

fun loadPropertiesFile(path: String): PropertiesSource = PropertiesSource.fromFile(path)

package io.github.fioldev.konfigure.sources.source

import io.github.fioldev.konfigure.core.KeyFormat
import io.github.fioldev.konfigure.core.MapBackedSource
import io.github.fioldev.konfigure.sources.parser.PropertiesParser
import io.github.fioldev.konfigure.sources.readFileText

class PropertiesSource(properties: Map<String, String>) : MapBackedSource(properties) {
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE)

    companion object {
        fun fromString(content: String): PropertiesSource =
            PropertiesSource(PropertiesParser.parse(content))

        fun fromFile(path: String): PropertiesSource =
            fromString(readFileText(path))
    }
}

fun loadPropertiesFile(path: String): PropertiesSource = PropertiesSource.fromFile(path)

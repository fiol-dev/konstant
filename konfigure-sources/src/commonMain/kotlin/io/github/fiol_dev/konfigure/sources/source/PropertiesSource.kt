package io.github.fiol_dev.konfigure.sources.source

import io.github.fiol_dev.konfigure.core.KeyFormat
import io.github.fiol_dev.konfigure.core.MapBackedSource
import io.github.fiol_dev.konfigure.sources.parser.PropertiesParser
import io.github.fiol_dev.konfigure.sources.readFileText

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

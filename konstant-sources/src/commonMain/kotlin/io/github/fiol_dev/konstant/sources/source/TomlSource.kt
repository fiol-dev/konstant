package io.github.fiol_dev.konstant.sources.source

import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.core.MapBackedSource
import io.github.fiol_dev.konstant.sources.parser.TomlParser
import io.github.fiol_dev.konstant.sources.readFileText

class TomlSource(entries: Map<String, String>) : MapBackedSource(entries) {
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE)

    companion object {
        fun fromString(content: String): TomlSource =
            TomlSource(TomlParser.parse(content))

        fun fromFile(path: String): TomlSource =
            fromString(readFileText(path))
    }
}

fun loadTomlFile(path: String): TomlSource = TomlSource.fromFile(path)

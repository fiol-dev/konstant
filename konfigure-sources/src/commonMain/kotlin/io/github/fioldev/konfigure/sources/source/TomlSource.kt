package io.github.fioldev.konfigure.sources.source

import io.github.fioldev.konfigure.core.KeyFormat
import io.github.fioldev.konfigure.core.MapBackedSource
import io.github.fioldev.konfigure.sources.parser.TomlParser
import io.github.fioldev.konfigure.sources.readFileText

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

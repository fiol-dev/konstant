package io.github.fioldev.konfigure.sources.source

import io.github.fioldev.konfigure.core.KeyFormat
import io.github.fioldev.konfigure.core.MapBackedSource
import io.github.fioldev.konfigure.sources.parser.YamlParser
import io.github.fioldev.konfigure.sources.readFileText

class YamlSource(entries: Map<String, String>) : MapBackedSource(entries) {
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE)

    companion object {
        fun fromString(content: String): YamlSource =
            YamlSource(YamlParser.parse(content))

        fun fromFile(path: String): YamlSource =
            fromString(readFileText(path))
    }
}

fun loadYamlFile(path: String): YamlSource = YamlSource.fromFile(path)

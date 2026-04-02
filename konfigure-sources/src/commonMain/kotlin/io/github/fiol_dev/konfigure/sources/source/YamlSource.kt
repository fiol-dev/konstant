package io.github.fiol_dev.konfigure.sources.source

import io.github.fiol_dev.konfigure.core.KeyFormat
import io.github.fiol_dev.konfigure.core.MapBackedSource
import io.github.fiol_dev.konfigure.sources.parser.YamlParser
import io.github.fiol_dev.konfigure.sources.readFileText

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

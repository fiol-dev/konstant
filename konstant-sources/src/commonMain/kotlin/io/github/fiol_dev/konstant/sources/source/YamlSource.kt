package io.github.fiol_dev.konstant.sources.source

import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.core.MapBackedSource
import io.github.fiol_dev.konstant.sources.parser.YamlParser
import io.github.fiol_dev.konstant.sources.readFileText

public class YamlSource(entries: Map<String, String>) : MapBackedSource(entries) {
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE)

    public companion object {
        public fun fromString(content: String): YamlSource =
            YamlSource(YamlParser.parse(content))

        public fun fromFile(path: String): YamlSource =
            fromString(readFileText(path))
    }
}

public fun loadYamlFile(path: String): YamlSource = YamlSource.fromFile(path)

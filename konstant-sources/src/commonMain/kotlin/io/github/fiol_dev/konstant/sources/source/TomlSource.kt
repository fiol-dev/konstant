package io.github.fiol_dev.konstant.sources.source

import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.core.MapBackedSource
import io.github.fiol_dev.konstant.sources.parser.TomlParser
import io.github.fiol_dev.konstant.sources.readFileText

public class TomlSource(entries: Map<String, String>) : MapBackedSource(entries) {
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE)

    public companion object {
        public fun fromString(content: String): TomlSource =
            TomlSource(TomlParser.parse(content))

        public fun fromFile(path: String): TomlSource =
            fromString(readFileText(path))
    }
}

public fun loadTomlFile(path: String): TomlSource = TomlSource.fromFile(path)

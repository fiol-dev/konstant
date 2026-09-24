package io.github.fiol_dev.konstant.test

import io.github.fiol_dev.konstant.core.ConfigSource
import io.github.fiol_dev.konstant.core.KeyFormat

public class MapSource(
    private val values: Map<String, String>,
    override val keyFormat: KeyFormat = KeyFormat.RAW,
) : ConfigSource {
    override fun get(key: String): String? = values[key]

    public companion object {
        public fun of(vararg pairs: Pair<String, String>): MapSource =
            MapSource(mapOf(*pairs))

        public fun screamingSnake(vararg pairs: Pair<String, String>): MapSource =
            MapSource(mapOf(*pairs), KeyFormat.SCREAMING_SNAKE)

        public fun dotNotation(vararg pairs: Pair<String, String>): MapSource =
            MapSource(mapOf(*pairs), KeyFormat.DOT_NOTATION)
    }
}

package io.github.fiol_dev.konfigure.test

import io.github.fiol_dev.konfigure.core.ConfigSource
import io.github.fiol_dev.konfigure.core.KeyFormat

class MapSource(
    private val values: Map<String, String>,
    override val keyFormat: KeyFormat = KeyFormat.RAW,
) : ConfigSource {
    override fun get(key: String): String? = values[key]

    companion object {
        fun of(vararg pairs: Pair<String, String>): MapSource =
            MapSource(mapOf(*pairs))

        fun screamingSnake(vararg pairs: Pair<String, String>): MapSource =
            MapSource(mapOf(*pairs), KeyFormat.SCREAMING_SNAKE)

        fun dotNotation(vararg pairs: Pair<String, String>): MapSource =
            MapSource(mapOf(*pairs), KeyFormat.DOT_NOTATION)
    }
}

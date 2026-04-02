package io.github.fiol_dev.konfigure.sources.source

import io.github.fiol_dev.konfigure.core.ConfigSource
import io.github.fiol_dev.konfigure.core.KeyFormat
import io.github.fiol_dev.konfigure.sources.getEnvironmentVariable

class EnvSource : ConfigSource {
    override val keyFormat: KeyFormat = KeyFormat.SCREAMING_SNAKE
    override fun get(key: String): String? = getEnvironmentVariable(key)
}
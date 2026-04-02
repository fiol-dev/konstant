package io.github.fioldev.konfigure.sources.source

import io.github.fioldev.konfigure.core.ConfigSource
import io.github.fioldev.konfigure.core.KeyFormat
import io.github.fioldev.konfigure.sources.getEnvironmentVariable

class EnvSource : ConfigSource {
    override val keyFormat: KeyFormat = KeyFormat.SCREAMING_SNAKE
    override fun get(key: String): String? = getEnvironmentVariable(key)
}
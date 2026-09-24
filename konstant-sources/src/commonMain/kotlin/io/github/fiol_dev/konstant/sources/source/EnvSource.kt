package io.github.fiol_dev.konstant.sources.source

import io.github.fiol_dev.konstant.core.ConfigSource
import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.sources.getEnvironmentVariable

public class EnvSource : ConfigSource {
    override val keyFormat: KeyFormat = KeyFormat.SCREAMING_SNAKE
    override fun get(key: String): String? = getEnvironmentVariable(key)
}
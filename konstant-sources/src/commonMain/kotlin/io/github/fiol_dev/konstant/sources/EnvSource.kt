package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.core.ConfigSource
import io.github.fiol_dev.konstant.core.KeyFormat

/**
 * Reads environment variables, with keys in `SCREAMING_SNAKE_CASE` (`DATABASE_URL`). Names are
 * matched exactly, including case. Variables are read at each lookup, not copied up front. On
 * JS and Wasm it reads `process.env` under Node; browsers have no environment, so every lookup
 * there returns null.
 */
public class EnvSource : ConfigSource {
    override val keyFormat: KeyFormat = KeyFormat.SCREAMING_SNAKE
    override fun get(key: String): String? = getEnvironmentVariable(key)
}
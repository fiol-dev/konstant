package io.github.fiol_dev.konstant.sources

internal actual fun getEnvironmentVariable(key: String): String? = System.getenv(key)

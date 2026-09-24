package io.github.fiol_dev.konstant.sources

actual fun getEnvironmentVariable(key: String): String? = System.getenv(key)

package io.github.fiol_dev.konfigure.sources

actual fun getEnvironmentVariable(key: String): String? = System.getenv(key)

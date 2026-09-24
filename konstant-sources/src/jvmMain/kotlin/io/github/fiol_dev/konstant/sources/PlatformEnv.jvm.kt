package io.github.fiol_dev.konstant.sources

public actual fun getEnvironmentVariable(key: String): String? = System.getenv(key)

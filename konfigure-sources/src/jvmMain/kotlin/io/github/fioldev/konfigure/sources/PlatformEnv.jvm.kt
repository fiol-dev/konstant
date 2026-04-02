package io.github.fioldev.konfigure.sources

actual fun getEnvironmentVariable(key: String): String? = System.getenv(key)

package io.github.fioldev.konfigure.sources

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
actual fun getEnvironmentVariable(key: String): String? = getenv(key)?.toKString()

package io.github.fiol_dev.konstant.sources

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
public actual fun getEnvironmentVariable(key: String): String? = getenv(key)?.toKString()

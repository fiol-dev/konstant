package io.github.fiol_dev.konstant.sources

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.F_OK
import platform.posix.access

@OptIn(ExperimentalForeignApi::class)
public actual fun readResourceText(path: String): String? =
    if (access(path, F_OK) == 0) readFileText(path) else null

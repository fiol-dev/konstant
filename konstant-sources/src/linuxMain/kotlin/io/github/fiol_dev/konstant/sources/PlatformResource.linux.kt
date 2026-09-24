package io.github.fiol_dev.konstant.sources

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.F_OK
import platform.posix.access

// Resource paths are relative on every platform, so a leading "/" is dropped as on JVM and Android
@OptIn(ExperimentalForeignApi::class)
public actual fun readResourceText(path: String): String? {
    val file = path.removePrefix("/")
    return if (access(file, F_OK) == 0) readFileText(file) else null
}

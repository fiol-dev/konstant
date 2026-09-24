@file:OptIn(ExperimentalForeignApi::class)

package io.github.fiol_dev.konstant.sources

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell

actual fun readFileText(path: String): String {
    val file = fopen(path, "r") ?: throw IllegalArgumentException("Cannot open file: $path")
    try {
        fseek(file, 0, SEEK_END)
        val size = ftell(file)
        fseek(file, 0, SEEK_SET)
        val buffer = ByteArray(size.toInt())
        fread(buffer.refTo(0), 1u, size.toULong(), file)
        return buffer.toKString()
    } finally {
        fclose(file)
    }
}

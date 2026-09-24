@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, InternalKonstantApi::class)

package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.core.InternalKonstantApi

private fun hasNodeFs(): Boolean =
    js("typeof process !== 'undefined' && typeof process.getBuiltinModule === 'function'")

private fun nodeFileExists(path: String): Boolean =
    js("process.getBuiltinModule('fs').existsSync(path)")

private fun nodeReadFile(path: String): String =
    js("process.getBuiltinModule('fs').readFileSync(path, 'utf8')")

@InternalKonstantApi
public actual fun readFileText(path: String): String {
    if (!hasNodeFs()) throw UnsupportedOperationException(
        "Files can't be read in the browser. Bake config into the app with the Konstant Gradle plugin, " +
            "or pass the text to a source's fromString."
    )
    // Checked first so a missing file is a Kotlin exception rather than a JsException
    if (!nodeFileExists(path)) throw IllegalArgumentException("File not found: $path")
    return nodeReadFile(path)
}

// Resource paths are relative on every platform, so a leading "/" is dropped as on JVM and Android.
// Browsers have no file system, so nothing is found there.
@InternalKonstantApi
public actual fun readResourceText(path: String): String? {
    if (!hasNodeFs()) return null
    val file = path.removePrefix("/")
    return if (nodeFileExists(file)) nodeReadFile(file) else null
}

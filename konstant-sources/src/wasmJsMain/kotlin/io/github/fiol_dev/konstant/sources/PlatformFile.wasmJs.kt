@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.fiol_dev.konstant.sources

private fun hasNodeFs(): Boolean =
    js("typeof process !== 'undefined' && typeof process.getBuiltinModule === 'function'")

private fun nodeFileExists(path: String): Boolean =
    js("process.getBuiltinModule('fs').existsSync(path)")

private fun nodeReadFile(path: String): String =
    js("process.getBuiltinModule('fs').readFileSync(path, 'utf8')")

public actual fun readFileText(path: String): String {
    if (!hasNodeFs()) throw UnsupportedOperationException(
        "Files can't be read in the browser. Bake config into the app with the Konstant Gradle plugin, " +
            "or pass the text to a source's fromString."
    )
    return nodeReadFile(path)
}

// Resource paths are relative on every platform, so a leading "/" is dropped as on JVM and Android.
// Browsers have no file system, so nothing is found there.
public actual fun readResourceText(path: String): String? {
    if (!hasNodeFs()) return null
    val file = path.removePrefix("/")
    return if (nodeFileExists(file)) nodeReadFile(file) else null
}

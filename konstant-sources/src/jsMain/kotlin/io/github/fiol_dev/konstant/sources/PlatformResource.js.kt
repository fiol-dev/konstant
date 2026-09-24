package io.github.fiol_dev.konstant.sources

// Resource paths are relative on every platform, so a leading "/" is dropped as on JVM and Android.
// Browsers have no file system, so nothing is found there.
public actual fun readResourceText(path: String): String? {
    val fs = nodeFs() ?: return null
    val file = path.removePrefix("/")
    return if (fs.existsSync(file) as Boolean) readFileText(file) else null
}

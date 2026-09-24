package io.github.fiol_dev.konstant.sources

// Resource paths are relative on every platform, so a leading "/" is dropped as on JVM and Android
public actual fun readResourceText(path: String): String? {
    val file = path.removePrefix("/")
    val fs = js("require('fs')")
    return if (fs.existsSync(file) as Boolean) readFileText(file) else null
}

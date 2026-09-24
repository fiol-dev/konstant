package io.github.fiol_dev.konstant.sources

public actual fun readResourceText(path: String): String? {
    val fs = js("require('fs')")
    return if (fs.existsSync(path) as Boolean) readFileText(path) else null
}

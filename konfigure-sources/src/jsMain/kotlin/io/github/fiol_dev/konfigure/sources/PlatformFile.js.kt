package io.github.fiol_dev.konfigure.sources

actual fun readFileText(path: String): String {
    val fs = js("require('fs')")
    return fs.readFileSync(path, "utf8") as String
}

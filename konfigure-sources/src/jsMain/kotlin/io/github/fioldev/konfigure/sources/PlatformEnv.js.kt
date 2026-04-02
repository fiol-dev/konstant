package io.github.fioldev.konfigure.sources

actual fun getEnvironmentVariable(key: String): String? {
    return js("process.env[key]") as? String
}

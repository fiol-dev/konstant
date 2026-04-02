package io.github.fiol_dev.konfigure.sources

actual fun getEnvironmentVariable(key: String): String? {
    return js("process.env[key]") as? String
}

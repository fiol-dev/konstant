package io.github.fiol_dev.konstant.sources

public actual fun getEnvironmentVariable(key: String): String? {
    return js("process.env[key]") as? String
}

package io.github.fiol_dev.konstant.sources

// Browsers have no environment variables, so every lookup there is null
internal actual fun getEnvironmentVariable(key: String): String? {
    return js("(typeof process !== 'undefined' && process.env) ? process.env[key] : undefined") as? String
}

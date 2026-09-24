package io.github.fiol_dev.konstant.sources

// Browsers have no environment variables, so every lookup there is null
private fun envValue(key: String): String? =
    js("(typeof process !== 'undefined' && process.env && typeof process.env[key] === 'string') ? process.env[key] : null")

public actual fun getEnvironmentVariable(key: String): String? = envValue(key)

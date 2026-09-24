@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.sources.source

import io.github.fiol_dev.konstant.core.InternalKonstantApi
import io.github.fiol_dev.konstant.sources.readResourceText
import io.github.fiol_dev.konstant.sources.requireResourceText

// Loaders for config files bundled with the app, see readResourceText for where each platform
// looks. With optional = true a missing file gives an empty source instead of an error, which
// suits per-environment overrides such as "config.local.properties". TOML, YAML and JSON files
// load through TomlSource.fromResource and friends in konstant-toml, konstant-yaml and konstant-json.

public fun loadPropertiesResource(path: String, optional: Boolean = false): PropertiesSource =
    PropertiesSource.fromString(resourceText(path, optional))

public fun loadDotEnvResource(path: String, optional: Boolean = false): DotEnvSource =
    DotEnvSource.fromString(resourceText(path, optional))

private fun resourceText(path: String, optional: Boolean): String =
    if (optional) readResourceText(path).orEmpty() else requireResourceText(path)

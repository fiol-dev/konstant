package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.sources.source.PropertiesSource
import java.io.File

public actual fun readFileText(path: String): String = File(path).readText()

@Deprecated(
    "Moved to the common source package and now works on every platform.",
    ReplaceWith("loadPropertiesResource(resource)", "io.github.fiol_dev.konstant.sources.source.loadPropertiesResource"),
)
public fun loadPropertiesResource(resource: String): PropertiesSource =
    io.github.fiol_dev.konstant.sources.source.loadPropertiesResource(resource)

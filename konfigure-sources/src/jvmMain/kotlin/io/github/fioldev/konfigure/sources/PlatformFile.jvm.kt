package io.github.fioldev.konfigure.sources

import io.github.fioldev.konfigure.sources.source.PropertiesSource
import java.io.File

actual fun readFileText(path: String): String = File(path).readText()

@Suppress("unused")
fun loadPropertiesResource(resource: String): PropertiesSource {
    val content = Thread.currentThread().contextClassLoader
        .getResourceAsStream(resource)
        ?.bufferedReader()
        ?.readText()
        ?: throw IllegalArgumentException("Classpath resource not found: $resource")
    return PropertiesSource.fromString(content)
}
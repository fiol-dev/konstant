package io.github.fiol_dev.konstant.sources

/**
 * Reads a text file shipped inside the app, or null if there is none at [path]:
 * - Android: `src/main/assets/<path>` (call [initKonstantAndroid] only if the automatic init
 *   is disabled)
 * - iOS/macOS: the main bundle's resources
 * - JVM: the classpath (`src/main/resources/<path>`)
 * - JS (Node) and Linux: a file at [path], relative to the working directory
 */
public expect fun readResourceText(path: String): String?

internal fun requireResourceText(path: String): String =
    readResourceText(path) ?: throw IllegalArgumentException("Bundled resource not found: $path")

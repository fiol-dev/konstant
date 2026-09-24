package io.github.fiol_dev.konstant.sources

import java.io.File

actual fun readFileText(path: String): String = File(path).readText()

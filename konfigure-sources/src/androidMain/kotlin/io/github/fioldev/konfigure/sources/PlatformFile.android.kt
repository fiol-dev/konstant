package io.github.fioldev.konfigure.sources

import java.io.File

actual fun readFileText(path: String): String = File(path).readText()

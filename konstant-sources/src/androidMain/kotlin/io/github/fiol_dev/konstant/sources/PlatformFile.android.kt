@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.core.InternalKonstantApi
import java.io.File

@InternalKonstantApi
public actual fun readFileText(path: String): String = File(path).readText()

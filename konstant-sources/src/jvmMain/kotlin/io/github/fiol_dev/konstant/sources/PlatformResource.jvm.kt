@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.core.InternalKonstantApi

@InternalKonstantApi
public actual fun readResourceText(path: String): String? {
    val loader = Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()
    return loader.getResourceAsStream(path.removePrefix("/"))?.bufferedReader()?.use { it.readText() }
}

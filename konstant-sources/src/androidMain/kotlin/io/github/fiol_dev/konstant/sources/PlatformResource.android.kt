@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.sources

import android.content.Context
import io.github.fiol_dev.konstant.core.InternalKonstantApi
import java.io.FileNotFoundException

@Volatile
private var appContext: Context? = null

/**
 * Gives Konstant the application context for reading assets. Needed only for `fromResource`
 * sources: call it in `Application.onCreate` before loading, or register [KonstantInitProvider]
 * in your manifest to do it at app start.
 */
public fun initKonstantAndroid(context: Context) {
    appContext = context.applicationContext ?: context
}

@InternalKonstantApi
public actual fun readResourceText(path: String): String? {
    val context = appContext ?: throw IllegalStateException(
        "Konstant has no Android context yet. Call initKonstantAndroid(context) in Application.onCreate " +
            "before loading resources, or register KonstantInitProvider in your manifest."
    )
    return try {
        context.assets.open(path.removePrefix("/")).bufferedReader().use { it.readText() }
    } catch (_: FileNotFoundException) {
        null
    }
}

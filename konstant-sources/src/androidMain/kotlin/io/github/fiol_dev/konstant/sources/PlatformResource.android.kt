package io.github.fiol_dev.konstant.sources

import android.content.Context
import java.io.FileNotFoundException

@Volatile
private var appContext: Context? = null

/**
 * Gives Konstant the application context for reading assets. The library's manifest registers
 * [KonstantInitProvider], which calls this at app start, so you only need it when that provider
 * is removed or in unit tests.
 */
public fun initKonstantAndroid(context: Context) {
    appContext = context.applicationContext ?: context
}

public actual fun readResourceText(path: String): String? {
    val context = appContext ?: throw IllegalStateException(
        "Konstant has no Android context yet. Call initKonstantAndroid(context) before loading resources."
    )
    return try {
        context.assets.open(path.removePrefix("/")).bufferedReader().use { it.readText() }
    } catch (_: FileNotFoundException) {
        null
    }
}

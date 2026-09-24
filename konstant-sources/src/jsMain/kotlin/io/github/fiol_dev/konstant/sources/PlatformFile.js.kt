@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.core.InternalKonstantApi

// No literal require('fs'): browser bundlers would try to resolve 'fs' and fail the build
// (webpack also rewrites `typeof require`), so the old-Node fallback is guarded by try instead
internal fun nodeFs(): dynamic = js(
    "(function () {" +
        " if (typeof process !== 'undefined' && process.getBuiltinModule) return process.getBuiltinModule('fs');" +
        " try { return eval('require')('fs'); } catch (e) { return null; }" +
        " })()"
)

@InternalKonstantApi
public actual fun readFileText(path: String): String {
    val fs = nodeFs() ?: throw UnsupportedOperationException(BROWSER_FILES_MESSAGE)
    return fs.readFileSync(path, "utf8") as String
}

internal const val BROWSER_FILES_MESSAGE: String =
    "Files can't be read in the browser. Bake config into the app with the Konstant Gradle plugin, " +
        "or pass the text to a source's fromString."

@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.core.InternalKonstantApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@InternalKonstantApi
public actual fun readResourceText(path: String): String? {
    val file = path.removePrefix("/")
    val name = file.substringBeforeLast('.')
    val type = file.substringAfterLast('.', "").ifEmpty { null }
    val resolved = NSBundle.mainBundle.pathForResource(name, type) ?: return null
    return NSString.stringWithContentsOfFile(resolved, NSUTF8StringEncoding, null)
}

@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.core.InternalKonstantApi

/**
 * Reads the text file at [path]. Throws if it cannot be read; in browsers, which have no file
 * system, it always throws. Used by the sources' `fromFile` functions.
 */
@InternalKonstantApi
public expect fun readFileText(path: String): String

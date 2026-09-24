@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.core.InternalKonstantApi

@InternalKonstantApi
public expect fun readFileText(path: String): String

package io.github.fiol_dev.konstant.ksptests

import io.github.fiol_dev.konstant.annotations.ConfigSpec
import io.github.fiol_dev.konstant.annotations.Key
import io.github.fiol_dev.konstant.annotations.Secret

@ConfigSpec
data class PrimitivesConfig(
    val name: String,
    val count: Int,
    val bigCount: Long,
    val ratio: Double,
    val scale: Float,
    val enabled: Boolean,
)

@ConfigSpec
data class DefaultsConfig(
    val host: String = "localhost",
    val port: Int = 8080,
    val timeoutMs: Long = 60_000L,
    val ratio: Double = 0.5,
    val label: String = "a, (b) = \"c\"",
    val debug: Boolean = false,
)

@ConfigSpec
data class AnnotatedConfig(
    @Key("CUSTOM_API_KEY") val apiKey: String,
    @Secret val password: String,
    @Secret val pin: Int = 0,
)

@ConfigSpec
data class DatabaseConfig(
    val url: String,
    val maxPoolSize: Int = 10,
)

@ConfigSpec
data class AppConfig(
    val appName: String = "MyApp",
    val database: DatabaseConfig,
)

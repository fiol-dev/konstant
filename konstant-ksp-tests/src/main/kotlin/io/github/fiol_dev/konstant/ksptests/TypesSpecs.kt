package io.github.fiol_dev.konstant.ksptests

import io.github.fiol_dev.konstant.annotations.ConfigSpec
import io.github.fiol_dev.konstant.ksptests.other.CacheConfig
import io.github.fiol_dev.konstant.ksptests.other.Mode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class LogLevel { DEBUG, INFO, WARN_ONLY }

@ConfigSpec
data class TypesConfig(
    val level: LogLevel,
    val timeout: Duration,
    val hosts: List<String>,
    val ports: Set<Int>,
    val weights: Map<String, Double>,
    val levels: List<LogLevel> = emptyList(),
    val nickname: String?,
    val retries: Int? = 3,
    val fallbackLevel: LogLevel = LogLevel.INFO,
    val grace: Duration = 5.seconds,
)

// Nested spec and default expression both come from another package
@ConfigSpec
data class ServiceConfig(
    val name: String = "svc",
    val mode: Mode = Mode.FIFO,
    val cache: CacheConfig,
)

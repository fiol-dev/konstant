package io.github.fiol_dev.konstant.ksptests

import io.github.fiol_dev.konstant.annotations.ConfigSpec
import io.github.fiol_dev.konstant.annotations.Convert
import io.github.fiol_dev.konstant.annotations.NotBlank
import io.github.fiol_dev.konstant.annotations.Pattern
import io.github.fiol_dev.konstant.annotations.Range
import io.github.fiol_dev.konstant.annotations.Secret
import io.github.fiol_dev.konstant.annotations.Size
import io.github.fiol_dev.konstant.core.ValueConverter

public data class HostPort(val host: String, val port: Int)

public object HostPortConverter : ValueConverter<HostPort> {
    override fun convert(raw: String): HostPort {
        val parts = raw.trim().split(':')
        require(parts.size == 2) { "expected host:port" }
        return HostPort(parts[0], parts[1].toInt())
    }
}

/** Accepts `75%` as well as `75`. */
public object PercentConverter : ValueConverter<Int> {
    override fun convert(raw: String): Int = raw.trim().removeSuffix("%").toInt()
}

@ConfigSpec
public data class ValidatedConfig(
    @Range(min = 1.0, max = 65535.0) val port: Int = 8080,
    @Range(min = 0.0) val ratio: Double = 0.5,
    @NotBlank @Size(max = 10) val name: String = "app",
    @Pattern("[a-z]+-\\d+") val code: String = "ab-1",
    @Size(min = 1) val hosts: List<String> = listOf("a"),
    @Secret @Size(min = 8) val token: String? = null,
    @Convert(HostPortConverter::class) val upstream: HostPort = HostPort("localhost", 80),
    @Convert(HostPortConverter::class) val backup: HostPort? = null,
    @Range(max = 0.1) val jitter: Float = 0.05f,
    @Convert(PercentConverter::class) @Range(max = 100.0) val share: Int = 50,
    val minWorkers: Int = 1,
    val maxWorkers: Int = 4,
) {
    init {
        require(minWorkers <= maxWorkers) { "minWorkers must not exceed maxWorkers" }
    }
}

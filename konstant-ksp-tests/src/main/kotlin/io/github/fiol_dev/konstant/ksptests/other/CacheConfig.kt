package io.github.fiol_dev.konstant.ksptests.other

import io.github.fiol_dev.konstant.annotations.ConfigSpec

enum class Mode { LRU, FIFO }

@ConfigSpec
data class CacheConfig(
    val size: Int = 100,
    val mode: Mode = Mode.LRU,
)

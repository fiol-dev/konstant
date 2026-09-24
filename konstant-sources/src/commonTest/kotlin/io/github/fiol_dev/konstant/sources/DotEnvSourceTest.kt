package io.github.fiol_dev.konstant.sources

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DotEnvSourceTest {

    @Test
    fun missingFileIsEmptyByDefault() {
        assertNull(DotEnvSource.fromFile("does-not-exist.env").get("ANY"))
    }

    @Test
    fun missingFileFailsWhenNotOptional() {
        assertFailsWith<Exception> { DotEnvSource.fromFile("does-not-exist.env", optional = false) }
    }
}

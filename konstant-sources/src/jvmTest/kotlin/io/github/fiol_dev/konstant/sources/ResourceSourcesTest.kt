package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.sources.source.loadPropertiesResource
import io.github.fiol_dev.konstant.sources.source.loadTomlResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ResourceSourcesTest {

    @Test
    fun readsTomlFromClasspath() {
        assertEquals("9090", loadTomlResource("konstant-test.toml").get("server.port"))
    }

    @Test
    fun missingResourceFailsUnlessOptional() {
        assertFailsWith<IllegalArgumentException> { loadTomlResource("missing.toml") }
        assertNull(loadPropertiesResource("missing.properties", optional = true).get("any"))
    }
}

package io.github.fiol_dev.konstant.sources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ResourceSourcesTest {

    @Test
    fun readsPropertiesFromClasspath() {
        assertEquals("9090", PropertiesSource.fromResource("konstant-test.properties").get("server.port"))
    }

    @Test
    fun missingResourceFailsUnlessOptional() {
        assertFailsWith<IllegalArgumentException> { PropertiesSource.fromResource("missing.properties") }
        assertNull(PropertiesSource.fromResource("missing.properties", optional = true).get("any"))
    }
}

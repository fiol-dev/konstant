package io.github.fiol_dev.konstant.test

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapSourceTest {

    @Test
    fun lookupFallsBackToCaseInsensitiveMatch() {
        val source = MapSource.of("Server.Port" to "8080")
        assertEquals("8080", source.get("Server.Port"))
        assertEquals("8080", source.get("server.port"))
        assertNull(source.get("server.host"))
    }

    @Test
    fun childrenListsNestedEntries() {
        val source = MapSource.of("labels.env" to "prod", "labels.team" to "core", "name" to "app")
        assertEquals(mapOf("env" to "prod", "team" to "core"), source.children("labels"))
        assertNull(source.children("name"))
    }

    @Test
    fun nameIsMapSource() {
        assertEquals("MapSource", MapSource.of().name)
    }
}
